import requests
import json
import sys

# Configuration
BASE_URL = "http://localhost:8000/api/v1/write/process"

def test_writing_instruction(instruction, text, context=None):
    print(f"\nTesting instruction: [{instruction}]")
    print(f"Input text: {text}")
    
    payload = {
        "text": text,
        "instruction": instruction,
        "context": context
    }
    
    try:
        response = requests.post(BASE_URL, json=payload, stream=True)
        
        if response.status_code == 200:
            print("Response stream:")
            full_response = ""
            for line in response.iter_lines():
                if line:
                    decoded_line = line.decode('utf-8')
                    if decoded_line.startswith("data: "):
                        data = decoded_line[6:]
                        if data == "[DONE]":
                            print("\n[Stream Complete]")
                            break
                        try:
                            # Tries to see if it's JSON (sometimes initial events are JSON)
                            json_data = json.loads(data)
                            print(f"[Event] {json_data}")
                        except json.JSONDecodeError:
                            # Otherwise it's text content
                            # decode escaped newlines
                            content = data.replace('\\n', '\n')
                            print(content, end="", flush=True)
                            full_response += content
            print("\n--------------")
        else:
            print(f"Error: Status Code {response.status_code}")
            print(response.text)
    except Exception as e:
        print(f"Request failed: {e}")

if __name__ == "__main__":
    # 1. Test Polish
    test_writing_instruction(
        "polish",
        "i think this user interface is good but maybe needs some color fixes."
    )

    # 2. Test Expand
    test_writing_instruction(
        "expand",
        "RAG technology combines retrieval and generation."
    )
    
    # 3. Test Fix Grammar
    test_writing_instruction(
        "fix_grammar",
        "Me goes to the store yesterday and buyed apples."
    )
