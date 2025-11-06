# Fix CUDA Out of Memory Error - Instructions

## Problem
IDM-VTON server starts successfully but fails during image processing with:
```
torch.OutOfMemoryError: CUDA out of memory
```

## Solution
Move DensePose preprocessing to CPU and enable memory optimizations.

---

## Option 1: Automated Fix (Recommended)

Run these commands on your EC2 instance (ubuntu@100.25.110.185):

```bash
# Step 1: Stop the current server (if running)
pkill -f "gradio_demo/app.py"

# Step 2: Navigate to IDM-VTON directory
cd ~/IDM-VTON

# Step 3: Backup original files
cp gradio_demo/app.py gradio_demo/app.py.backup
cp preprocess/apply_net.py preprocess/apply_net.py.backup

# Step 4: Modify DensePose to use CPU instead of GPU
cd ~/IDM-VTON/preprocess
sed -i 's/cfg.MODEL.DEVICE = "cuda"/cfg.MODEL.DEVICE = "cpu"/g' humanparsing/run_parsing.py
sed -i 's/device = torch.device("cuda")/device = torch.device("cpu")/g' openpose/run_openpose.py

# For DensePose - edit apply_net.py manually
nano apply_net.py
# Find line with: cfg.MODEL.DEVICE = "cuda" or similar
# Change to: cfg.MODEL.DEVICE = "cpu"
# Save with Ctrl+X, Y, Enter

# Step 5: Set memory optimization env vars and restart
cd ~/IDM-VTON
source venv/bin/activate
export PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512

# Step 6: Start server
python gradio_demo/app.py
```

---

## Option 2: Manual Fix

### A. Modify DensePose to use CPU

1. **SSH into EC2:**
   ```bash
   ssh -i ~/.ssh/tryon-key.pem ubuntu@100.25.110.185
   ```

2. **Stop running server:**
   ```bash
   pkill -f "gradio_demo/app.py"
   ```

3. **Edit DensePose configuration:**
   ```bash
   cd ~/IDM-VTON/preprocess
   nano apply_net.py
   ```

   Find this line (around line 30-40):
   ```python
   cfg.MODEL.DEVICE = "cuda"
   ```

   Change to:
   ```python
   cfg.MODEL.DEVICE = "cpu"
   ```

   Save: Ctrl+X, Y, Enter

4. **Edit OpenPose configuration:**
   ```bash
   nano openpose/run_openpose.py
   ```

   Find:
   ```python
   device = torch.device("cuda")
   ```

   Change to:
   ```python
   device = torch.device("cpu")
   ```

   Save: Ctrl+X, Y, Enter

5. **Edit HumanParsing configuration:**
   ```bash
   nano humanparsing/run_parsing.py
   ```

   Find:
   ```python
   device = torch.device("cuda")
   ```

   Change to:
   ```python
   device = torch.device("cpu")
   ```

   Save: Ctrl+X, Y, Enter

### B. Restart with Memory Optimization

```bash
cd ~/IDM-VTON
source venv/bin/activate

# Set memory optimization
export PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512

# Start server
python gradio_demo/app.py
```

---

## What This Does

1. **Moves preprocessing to CPU**: DensePose, OpenPose, and HumanParsing now run on CPU (32GB RAM) instead of GPU (15GB VRAM)
2. **Keeps main model on GPU**: The UNet diffusion model stays on GPU for fast inference
3. **Optimizes memory allocation**: PYTORCH_CUDA_ALLOC_CONF prevents memory fragmentation

---

## Expected Results

After applying the fix:
- Server starts at http://100.25.110.185:7860 ✓
- Interface loads ✓
- User uploads images ✓
- **Processing completes successfully** ✓ (this should now work!)
- Result image generated ✓

Processing time may increase slightly (5-10 seconds) since preprocessing runs on CPU, but this is acceptable for the quality of results.

---

## Testing

1. Open: http://100.25.110.185:7860
2. Upload a person image
3. Upload a garment image
4. Click "Try-on"
5. Wait 20-40 seconds
6. Check server logs for any errors
7. Result should appear without CUDA OOM error

---

## If Still Getting Errors

### Error: Still out of memory
```bash
# Reduce image resolution in app.py
nano ~/IDM-VTON/gradio_demo/app.py

# Find the image preprocessing section and add:
# image = image.resize((768, 1024))  # Reduce from default resolution
```

### Error: Preprocessing too slow
- This is normal - CPU preprocessing takes longer
- Consider using g4dn.4xlarge (48GB RAM, 64 vCPUs) for faster CPU processing
- Or accept the slower processing time (30-45 seconds total)

---

## Rollback (if needed)

```bash
cd ~/IDM-VTON
cp gradio_demo/app.py.backup gradio_demo/app.py
cp preprocess/apply_net.py.backup preprocess/apply_net.py
```

---

## Cost Impact

No additional cost - still using g4dn.2xlarge ($0.752/hour)

The fix simply redistributes compute:
- **Before**: All on GPU (causes OOM)
- **After**: Preprocessing on CPU, main inference on GPU (works!)
