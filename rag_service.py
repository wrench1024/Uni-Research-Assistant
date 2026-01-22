"""
RAG Service Module
Handles document ingestion, embedding, and context retrieval using LangChain + pgvector.
"""
import os
from typing import List, Optional
from langchain_community.document_loaders import PyPDFLoader, TextLoader, UnstructuredMarkdownLoader, Docx2txtLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_postgres import PGVector
from dotenv import load_dotenv

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


def search_context(query: str, k: int = 8, doc_id: Optional[str] = None) -> List[str]:
    """
    Search for relevant context based on a query.
    
    Args:
        query: The user's question
        k: Number of chunks to retrieve (default: 8 for better coverage)
        doc_id: Optional filter by document ID
    
    Returns:
        List of relevant text chunks
    """
    print(f"Searching context for: {query[:100]}...")
    
    vector_store = get_vector_store()
    
    # Build filter if doc_id is provided
    filter_dict = None
    if doc_id:
        filter_dict = {"doc_id": doc_id}
    
    # Perform similarity search
    docs = vector_store.similarity_search(
        query,
        k=k,
        filter=filter_dict
    )
    
    # Log source documents for debugging
    for i, doc in enumerate(docs):
        source_id = doc.metadata.get('doc_id', 'unknown')
        print(f"  Chunk {i+1}: doc_id={source_id}, preview={doc.page_content[:50]}...")
    
    # Extract text content
    context_chunks = [doc.page_content for doc in docs]
    
    print(f"Found {len(context_chunks)} relevant chunks")
    
    return context_chunks


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
    
    context_text = "\n\n---\n\n".join(context_chunks)
    
    system_prompt = f"""你是一个智能研究助手。请根据以下参考资料回答用户的问题。
如果参考资料中没有相关信息，请明确告知用户，并基于你的通用知识提供帮助。

【参考资料】
{context_text}

【注意事项】
1. 优先使用参考资料中的信息回答问题
2. 如果引用了参考资料，请注明来源
3. 如果参考资料不足以回答问题，请告知用户并提供建议"""
    
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
