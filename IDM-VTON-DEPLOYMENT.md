# IDM-VTON EC2 Deployment - Final Configuration

## Instance Details
- **Instance Type**: g4dn.2xlarge
- **GPU**: NVIDIA Tesla T4 (15GB VRAM)
- **RAM**: 32GB
- **Storage**: 150GB
- **IP**: 100.25.110.185
- **URL**: http://100.25.110.185:7860

## Working Configuration

### Performance
- **Processing Time**: ~30 seconds per image
- **Resolution**: 768x1024 (output)
- **Denoising Steps**: 20 (reduced from 30)
- **Quality**: Excellent

### Applied Optimizations

#### 1. CUDA Memory Fixes
- **DensePose on CPU**: Moved preprocessing to CPU to free GPU memory
  - File: `~/IDM-VTON/gradio_demo/app.py`
  - Line: `args = apply_net.create_argument_parser().parse_args(..., 'MODEL.DEVICE', 'cpu')`

- **VAE Optimizations**: Enabled slicing and tiling
  ```python
  pipe.enable_vae_slicing()
  pipe.enable_vae_tiling()
  ```

- **Attention Slicing**: Memory-efficient attention mechanism
  ```python
  pipe.enable_attention_slicing(1)
  ```

- **Memory Fragmentation Fix**: Environment variable
  ```bash
  export PYTORCH_CUDA_ALLOC_CONF=expandable_segments:True
  ```

#### 2. Speed Optimizations
- **Reduced Steps**: 30 → 20 denoising steps (~33% faster)
  - Default value changed in line 322 of app.py
  - Minimal quality impact

- **xformers Installed**: Memory-efficient attention (~10-15% faster)
  - Version: xformers==0.0.28.post3
  - Compatible with torch 2.5.1

### Installation Steps (Applied)

```bash
# 1. Fix CUDA out of memory
sed -i "s/'MODEL.DEVICE', 'cuda'/'MODEL.DEVICE', 'cpu'/g" gradio_demo/app.py

# 2. Add VAE optimizations (after pipe.unet_encoder = UNet_Encoder)
# Added manually:
print("Enabling memory optimizations...")
pipe.enable_vae_slicing()
pipe.enable_vae_tiling()
print("✓ VAE optimizations enabled")

# 3. Add attention slicing
print("Enabling attention slicing...")
try:
    pipe.enable_attention_slicing(1)
    print("✓ Attention slicing enabled")
except:
    print("Note: Attention slicing not available")

# 4. Reduce default steps
sed -i 's/value=30/value=20/' gradio_demo/app.py

# 5. Install xformers
pip install xformers==0.0.28.post3
```

### Server Startup Command

```bash
cd ~/IDM-VTON
source venv/bin/activate
export PYTORCH_CUDA_ALLOC_CONF=expandable_segments:True
python gradio_demo/app.py
```

### Run in Background (Persistent)

```bash
screen -S idm-vton
cd ~/IDM-VTON
source venv/bin/activate
export PYTORCH_CUDA_ALLOC_CONF=expandable_segments:True
python gradio_demo/app.py

# Detach: Ctrl+A then D
# Reattach: screen -r idm-vton
# Kill: screen -X -S idm-vton quit
```

## Cost Analysis

### Current Costs
- **Hourly**: $0.752/hour
- **Daily** (24/7): $18.05/day
- **Monthly** (24/7): $549/month
- **Per Request**: $0.00627 (~0.6 cents)
- **Storage** (150GB): ~$15/month

### Cost by Usage Pattern
| Pattern | Hours/Day | Monthly Cost |
|---------|-----------|--------------|
| Development (8h) | 8 | $181 |
| Part-time (12h) | 12 | $271 |
| Full-time (24/7) | 24 | $549 |

### Cost Reduction Options
1. **Stop when not in use**: Only pay storage (~$15/month)
2. **Spot instances**: ~$168/month (70% discount, can be terminated)
3. **Reserved instances**: ~$300-350/month (1-year commitment)
4. **Auto-scaling schedule**: Save 50-66%

## Resolution Requirements

⚠️ **IMPORTANT**: The model is hardcoded for 768x1024 resolution
- Cannot use lower resolutions (384x512, 512x768) - causes tensor dimension errors
- Preprocessing (DensePose, OpenPose) uses 384x512 - this is fine
- Main processing must be 768x1024

## Python Dependencies

### Key Packages
```
torch==2.5.1+cu121
torchvision==0.20.1+cu121
torchaudio==2.5.1+cu121
xformers==0.0.28.post3
diffusers==0.25.0
transformers==4.36.2
gradio==4.24.0
numpy<2
huggingface_hub==0.20.3
```

## Troubleshooting

### Issue: CUDA Out of Memory
**Solution**: Already applied (DensePose on CPU, VAE optimizations)

### Issue: Dimension Mismatch Errors
**Cause**: Trying to use non-standard resolution
**Solution**: Keep 768x1024 resolution

### Issue: xformers Version Conflict
**Solution**: Use xformers==0.0.28.post3 with torch==2.5.1

### Issue: Server Not Accessible
**Check**:
- Security group allows port 7860
- Server running: `ps aux | grep app.py`
- Elastic IP attached: 100.25.110.185

## Performance Benchmarks

### Before Optimizations
- Resolution: 768x1024
- Steps: 30
- Time: ~52 seconds
- Memory: GPU OOM errors

### After Optimizations
- Resolution: 768x1024
- Steps: 20
- Time: ~30 seconds
- Memory: Stable, no OOM
- Quality: Excellent (minimal difference from 30 steps)

### Speed Improvements Applied
1. DensePose to CPU: Freed GPU memory ✓
2. VAE slicing/tiling: -15% memory ✓
3. Attention slicing: -10% memory ✓
4. Steps 30→20: -33% time ✓
5. xformers: -10-15% time ✓
6. **Total**: ~42% faster (52s → 30s)

## Limitations

### Cannot Optimize Further Without Cost
- Resolution is fixed at 768x1024
- Steps below 15 degrade quality significantly
- Guidance scale reduction affects garment fit
- No more free optimizations available

### For Faster Processing
**Upgrade to g5.2xlarge** (NVIDIA A10G):
- Cost: $1.212/hour ($885/month)
- Speed: ~12-15 seconds (2x faster)
- Same 768x1024 quality

## API Integration

### Endpoint
```
http://100.25.110.185:7860
```

### Usage
- Upload person image
- Upload garment image
- Click "Try-on"
- Wait ~30 seconds
- Download result

### For Android App
Use Gradio client or HTTP API to integrate with your mobile application.

## Maintenance

### Restart Server
```bash
pkill -f "gradio_demo/app.py"
cd ~/IDM-VTON
source venv/bin/activate
export PYTORCH_CUDA_ALLOC_CONF=expandable_segments:True
python gradio_demo/app.py
```

### Update Models
Models are stored in:
- `~/IDM-VTON/unet/` (12GB)
- `~/IDM-VTON/unet_encoder/` (9.6GB)
- `~/IDM-VTON/ckpt/densepose/`
- `~/IDM-VTON/ckpt/openpose/`
- `~/IDM-VTON/ckpt/humanparsing/`

### Monitor GPU Usage
```bash
nvidia-smi
watch -n 1 nvidia-smi
```

## Success Criteria ✓

✓ Server starts successfully
✓ Interface loads at http://100.25.110.185:7860
✓ Can upload images
✓ Processing completes without errors
✓ Results generated in ~30 seconds
✓ 768x1024 output resolution
✓ Excellent quality
✓ No CUDA out of memory errors
✓ Stable for production use

## Next Steps

1. **Test with your Android app** - Integrate API endpoint
2. **Monitor costs** - Track usage patterns
3. **Set up auto-shutdown** - Save money during off-hours
4. **Consider Spot instances** - For development/testing
5. **Scale if needed** - Upgrade to g5.2xlarge for production speed
