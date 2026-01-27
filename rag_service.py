"""
RAG Service Module
Handles document ingestion, embedding, and context retrieval using LangChain + pgvector.
Supports hybrid retrieval (BM25 + Vector).
"""
import os
from typing import List, Optional, Dict, Tuple
from collections import defaultdict
from langchain_community.document_loaders import PyPDFLoader, TextLoader, UnstructuredMarkdownLoader, Docx2txtLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_postgres import PGVector
from dotenv import load_dotenv
from rank_bm25 import BM25Okapi
import jieba

load_dotenv()

# Database connection string for pgvector
CONNECTION_STRING = os.getenv(
    "PGVECTOR_CONNECTION_STRING",
    "postgresql://postgres:root@localhost:5432/vector_db"
)

# Embedding model - using a lightweight multilingual model
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")

# Collection name for storing vectors
COLLECTION_NAME = "research_documents"

# Global embedding instance (lazy loaded)
_embeddings = None

# Global BM25 index (lazy loaded)
_bm25_index = None
_bm25_corpus = []  # List of (doc_obj, tokenized_text) tuples

def get_embeddings():
    """Get or create the embedding model instance."""
    global _embeddings
    if _embeddings is None:
        print(f"Loading embedding model: {EMBEDDING_MODEL}")
        _embeddings = HuggingFaceEmbeddings(
            model_name=EMBEDDING_MODEL,
            model_kwargs={'device': 'cuda'},  # Using CUDA for faster embeddings
            encode_kwargs={'normalize_embeddings': True}
        )
    return _embeddings


def get_vector_store():
    """Get or create the PGVector store instance."""
    return PGVector(
        connection=CONNECTION_STRING,
        collection_name=COLLECTION_NAME,
        embeddings=get_embeddings(),
    )


def ingest_document(file_path: str, doc_id: Optional[str] = None) -> dict:
    """
    Ingest a document into the vector database.
    
    Args:
        file_path: Path to the document file (PDF, MD, TXT)
        doc_id: Optional document ID for metadata
    
    Returns:
        dict with status and chunk count
    """
    print(f"Ingesting document: {file_path}")
    
    # 1. Load document based on file type
    ext = os.path.splitext(file_path)[1].lower()
    
    if ext == '.pdf':
        loader = PyPDFLoader(file_path)
    elif ext == '.md':
        loader = UnstructuredMarkdownLoader(file_path)
    elif ext in ['.txt', '.text']:
        loader = TextLoader(file_path, encoding='utf-8')
    elif ext in ['.doc', '.docx']:
        loader = Docx2txtLoader(file_path)
    else:
        raise ValueError(f"Unsupported file type: {ext}. Supported: pdf, md, txt, doc, docx")
    
    documents = loader.load()
    print(f"Loaded {len(documents)} pages/sections")
    
    # 2. 智能分块 - 根据文档大小自适应
    total_chars = sum(len(doc.page_content) for doc in documents)
    
    # 大文档使用更大的 chunk，小文档使用更细的 chunk
    if total_chars > 100000:  # > 100K 字符 (约 50 页)
        chunk_size = 2000
        chunk_overlap = 400
        print(f"Large document detected ({total_chars} chars), using larger chunks")
    elif total_chars > 50000:  # 50K - 100K 字符
        chunk_size = 1500
        chunk_overlap = 300
    else:
        chunk_size = 1000
        chunk_overlap = 200
    
    # 使用章节感知的分隔符（优先按标题、段落分割）
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        length_function=len,
        separators=[
            "\n## ",      # Markdown 二级标题
            "\n### ",     # Markdown 三级标题
            "\n# ",       # Markdown 一级标题
            "\n\n",       # 段落
            "\n",         # 换行
            "。",         # 中文句号
            ".",          # 英文句号
            " ",          # 空格
            ""            # 字符
        ]
    )
    chunks = text_splitter.split_documents(documents)
    print(f"Split into {len(chunks)} chunks (size={chunk_size}, overlap={chunk_overlap})")
    
    # 3. Add metadata with section info
    for i, chunk in enumerate(chunks):
        chunk.metadata["doc_id"] = doc_id or os.path.basename(file_path)
        chunk.metadata["chunk_index"] = i
        chunk.metadata["total_chunks"] = len(chunks)
        chunk.metadata["source_file"] = file_path
    
    # 4. Store in vector database
    vector_store = get_vector_store()
    vector_store.add_documents(chunks)
    
    print(f"Successfully ingested {len(chunks)} chunks into vector database")
    
    return {
        "status": "success",
        "chunks_created": len(chunks),
        "doc_id": doc_id or os.path.basename(file_path)
    }


def build_bm25_index():
    """
    Build BM25 index from all documents in vector store.
    Should be called after ingesting documents.
    """
    global _bm25_index, _bm25_corpus
    
    print("Building BM25 index...")
    vector_store = get_vector_store()
    
    # Retrieve all documents from vector store
    try:
        all_docs = vector_store.similarity_search("", k=10000)  # Get all docs
    except:
        all_docs = []
    
    if not all_docs:
        print("No documents found in vector store")
        return
    
    # Tokenize documents for BM25
    tokenized_corpus = []
    doc_objects = []
    
    for doc in all_docs:
        # Use jieba for Chinese text, split for English
        tokens = list(jieba.cut_for_search(doc.page_content))
        tokenized_corpus.append(tokens)
        doc_objects.append(doc)
    
    # Build BM25 index
    _bm25_index = BM25Okapi(tokenized_corpus)
    _bm25_corpus = list(zip(doc_objects, tokenized_corpus))
    
    print(f"BM25 index built with {len(_bm25_corpus)} documents")


def bm25_search(query: str, k: int = 8, doc_id: Optional[str] = None) -> List[Tuple[dict, float]]:
    """
    Perform BM25 search.
    
    Args:
        query: Search query
        k: Number of results to return
        doc_id: Optional filter by document ID
    
    Returns:
        List of (doc_dict, score) tuples
    """
    global _bm25_index, _bm25_corpus
    
    # Build index if not exists
    if _bm25_index is None:
        build_bm25_index()
    
    if not _bm25_corpus:
        return []
    
    # Tokenize query
    query_tokens = list(jieba.cut_for_search(query))
    
    # Get BM25 scores
    scores = _bm25_index.get_scores(query_tokens)
    
    # Create (doc, score) pairs
    doc_score_pairs = []
    for i, (doc, _) in enumerate(_bm25_corpus):
        # Apply doc_id filter if provided
        if doc_id and doc.metadata.get('doc_id') != doc_id:
            continue
        
        doc_dict = {
            "text": doc.page_content,
            "doc_id": doc.metadata.get('doc_id', 'unknown'),
            "chunk_index": doc.metadata.get('chunk_index', -1),
            "source_file": doc.metadata.get('source_file', '')
        }
        doc_score_pairs.append((doc_dict, scores[i]))
    
    # Sort by score (descending) and return top k
    doc_score_pairs.sort(key=lambda x: x[1], reverse=True)
    return doc_score_pairs[:k]


def hybrid_search_rrf(query: str, k: int = 8, doc_id: Optional[str] = None) -> List[dict]:
    """
    Hybrid search using Reciprocal Rank Fusion (RRF).
    Combines BM25 and vector search results.
    
    Args:
        query: Search query
        k: Number of results to return
        doc_id: Optional filter by document ID
    
    Returns:
        List of document dictionaries
    """
    # Retrieve more candidates from each method
    k_candidates = k * 2
    
    # 1. Vector search
    vector_store = get_vector_store()
    filter_dict = {"doc_id": doc_id} if doc_id else None
    
    vector_docs = vector_store.similarity_search(
        query,
        k=k_candidates,
        filter=filter_dict
    )
    
    # 2. BM25 search
    bm25_results = bm25_search(query, k=k_candidates, doc_id=doc_id)
    
    # 3. RRF fusion
    # RRF formula: score = sum(1 / (k + rank_i))
    # where k is a constant (typically 60) and rank_i is the rank in retrieval i
    
    rrf_k = 60  # RRF constant
    doc_scores = defaultdict(float)
    doc_objects = {}  # Store document objects
    
    # Add vector search scores
    for rank, doc in enumerate(vector_docs, start=1):
        # Create unique key for deduplication
        key = (doc.metadata.get('doc_id', 'unknown'), doc.metadata.get('chunk_index', -1))
        doc_scores[key] += 1.0 / (rrf_k + rank)
        
        if key not in doc_objects:
            doc_objects[key] = {
                "text": doc.page_content,
                "doc_id": doc.metadata.get('doc_id', 'unknown'),
                "chunk_index": doc.metadata.get('chunk_index', -1),
                "source_file": doc.metadata.get('source_file', '')
            }
    
    # Add BM25 scores
    for rank, (doc_dict, _) in enumerate(bm25_results, start=1):
        key = (doc_dict['doc_id'], doc_dict['chunk_index'])
        doc_scores[key] += 1.0 / (rrf_k + rank)
        
        if key not in doc_objects:
            doc_objects[key] = doc_dict
    
    # Sort by RRF score and return top k
    sorted_docs = sorted(doc_scores.items(), key=lambda x: x[1], reverse=True)[:k]
    
    results = [doc_objects[key] for key, _ in sorted_docs]
    
    print(f"Hybrid search: vector={len(vector_docs)}, bm25={len(bm25_results)}, fused={len(results)}")
    
    return results


def search_context(query: str, k: int = 8, doc_id: Optional[str] = None, use_hybrid: bool = True) -> List[dict]:
    """
    Search for relevant context based on a query.
    Uses hybrid retrieval (BM25 + Vector) by default for better accuracy.
    
    Args:
        query: The user's question
        k: Number of chunks to retrieve (default: 8 for better coverage)
        doc_id: Optional filter by document ID
        use_hybrid: Whether to use hybrid retrieval (default: True)
    
    Returns:
        List of dictionaries containing text and metadata
    """
    print(f"Searching context for: {query[:100]}... (hybrid={use_hybrid})")
    
    # Use hybrid search if enabled
    if use_hybrid:
        results = hybrid_search_rrf(query, k=k, doc_id=doc_id)
    else:
        # Fallback to pure vector search
        vector_store = get_vector_store()
        filter_dict = {"doc_id": doc_id} if doc_id else None
        
        docs = vector_store.similarity_search(
            query,
            k=k,
            filter=filter_dict
        )
        
        results = []
        for doc in docs:
            results.append({
                "text": doc.page_content,
                "doc_id": doc.metadata.get('doc_id', 'unknown'),
                "chunk_index": doc.metadata.get('chunk_index', -1),
                "source_file": doc.metadata.get('source_file', '')
            })
    
    # Log source documents for debugging
    for i, result in enumerate(results):
        print(f"  Chunk {i+1}: doc_id={result['doc_id']}, idx={result['chunk_index']}, preview={result['text'][:50]}...")
    
    print(f"Found {len(results)} relevant chunks")
    
    return results


def build_rag_prompt(user_message: str, context_chunks: List[str]) -> str:
    """
    Build a RAG-enhanced system prompt.
    
    Args:
        user_message: The user's original message
        context_chunks: Retrieved context from vector database
    
    Returns:
        Enhanced prompt with context
    """
    if not context_chunks:
        return None  # No context available
    
    # Format context with explicit numbering [1], [2], etc.
    context_parts = []
    for i, chunk in enumerate(context_chunks):
        context_parts.append(f"【资料 {i+1}】\n{chunk}")
    
    context_text = "\n\n".join(context_parts)
    
    system_prompt = f"""你是一个智能研究助手。请仔细根据提供的【参考资料】回答用户问题。

【参考资料】
{context_text}

【回答要求】
1. **必须**明确引用参考资料，且仅在资料**真正有用**时引用。引用格式为 `[index]`。
   - 正确：根据资料 1，Transformer 模型... [1]
   - 错误：资料 1 是关于 Transformer 的，与问题无关 [1]。
2. 不要编造序号，只能使用提供的【资料 1】到【资料 {len(context_chunks)}】。
3. **关键**：如果参考资料与问题无关：
   - 直接说明“参考资料中没有相关信息”。
   - **严禁**描述参考资料的具体内容（例如“资料 1 讲了 XXX”），也**严禁**引用这些无关资料。
   - 直接基于你的通用知识回答用户问题。
4. 回答要逻辑清晰，结构化。"""
    
    return system_prompt


def delete_document_vectors(doc_id: str) -> dict:
    """
    Delete all vectors associated with a document.
    
    Args:
        doc_id: The document ID to delete
    
    Returns:
        dict with status and deleted count
    """
    print(f"Deleting vectors for document: {doc_id}")
    
    try:
        import psycopg2
        from urllib.parse import urlparse
        
        # Parse connection string
        parsed = urlparse(CONNECTION_STRING)
        conn = psycopg2.connect(
            host=parsed.hostname,
            port=parsed.port or 5432,
            database=parsed.path[1:],  # Remove leading '/'
            user=parsed.username,
            password=parsed.password
        )
        
        cursor = conn.cursor()
        
        # Delete vectors where metadata contains the doc_id
        delete_sql = """
            DELETE FROM langchain_pg_embedding 
            WHERE cmetadata->>'doc_id' = %s
        """
        cursor.execute(delete_sql, (str(doc_id),))
        deleted_count = cursor.rowcount
        
        conn.commit()
        cursor.close()
        conn.close()
        
        print(f"Successfully deleted {deleted_count} vectors for doc_id: {doc_id}")
        
        return {
            "status": "success",
            "deleted_count": deleted_count,
            "doc_id": doc_id
        }
    except Exception as e:
        print(f"Failed to delete vectors: {e}")
        return {
            "status": "error",
            "message": str(e),
            "doc_id": doc_id
        }


def get_document_chunks(doc_id: str) -> List[str]:
    """
    Retrieve all text chunks for a specific document, ordered by their index.
    Used for full-document tasks like summarization.
    
    Args:
        doc_id: The document ID to retrieve
    
    Returns:
        List of text chunks
    """
    print(f"Retrieving all chunks for document: {doc_id}")
    
    try:
        import psycopg2
        from urllib.parse import urlparse
        
        # Parse connection string
        parsed = urlparse(CONNECTION_STRING)
        conn = psycopg2.connect(
            host=parsed.hostname,
            port=parsed.port or 5432,
            database=parsed.path[1:],
            user=parsed.username,
            password=parsed.password
        )
        
        cursor = conn.cursor()
        
        # Query to get document content, ordered by chunk_index
        # Note: 'document' is the column name for the text content in langchain_pg_embedding
        select_sql = """
            SELECT document 
            FROM langchain_pg_embedding 
            WHERE cmetadata->>'doc_id' = %s 
            ORDER BY CAST(cmetadata->>'chunk_index' AS INTEGER) ASC
        """
        cursor.execute(select_sql, (str(doc_id),))
        rows = cursor.fetchall()
        
        chunks = [row[0] for row in rows]
        
        cursor.close()
        conn.close()
        
        print(f"Successfully retrieved {len(chunks)} chunks for doc_id: {doc_id}")
        return chunks
        
    except Exception as e:
        print(f"Failed to retrieve chunks: {e}")
        return []
