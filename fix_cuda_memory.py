#!/usr/bin/env python3
"""
Script to patch IDM-VTON app.py to enable GPU memory optimization
This adds CPU offloading to prevent CUDA out of memory errors
"""

import re

def patch_app_file(file_path):
    """Add enable_model_cpu_offload() after pipe initialization"""

    with open(file_path, 'r') as f:
        content = f.read()

    # Check if already patched
    if 'enable_model_cpu_offload' in content:
        print("✓ File already patched with CPU offloading")
        return False

    # Pattern to find where pipe is created/loaded
    # Look for lines like: pipe = AutoPipelineForInpainting.from_pretrained(...)
    # or similar pipeline initialization

    # Strategy 1: Add after pipe initialization
    pattern1 = r'(pipe\s*=\s*.*?from_pretrained.*?\))'
    if re.search(pattern1, content, re.DOTALL):
        modified_content = re.sub(
            pattern1,
            r'\1\n\n# Enable CPU offloading to save GPU memory\nprint("Enabling model CPU offloading...")\npipe.enable_model_cpu_offload()\nprint("✓ CPU offloading enabled")',
            content,
            count=1,
            flags=re.DOTALL
        )

        with open(file_path, 'w') as f:
            f.write(modified_content)
        print("✓ Successfully patched app.py with CPU offloading")
        return True

    # Strategy 2: Add after pipe.to(device)
    pattern2 = r'(pipe\.to\(["\']?cuda["\']?\))'
    if re.search(pattern2, content):
        modified_content = re.sub(
            pattern2,
            r'# pipe.to("cuda")  # Commented out - using CPU offloading instead\npipe.enable_model_cpu_offload()  # Enable CPU offloading to save GPU memory',
            content
        )

        with open(file_path, 'w') as f:
            f.write(modified_content)
        print("✓ Successfully patched app.py - replaced .to(device) with CPU offloading")
        return True

    print("⚠ Could not find suitable location to add CPU offloading")
    print("Manual patching may be required")
    return False

if __name__ == '__main__':
    import sys

    file_path = sys.argv[1] if len(sys.argv) > 1 else '/home/ubuntu/IDM-VTON/gradio_demo/app.py'

    print(f"Patching: {file_path}")
    print("-" * 60)

    try:
        patch_app_file(file_path)
    except Exception as e:
        print(f"✗ Error: {e}")
        sys.exit(1)
