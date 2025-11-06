# OOTDiffusion Integration Guide

## ✅ Integration Complete!

Your Android app is now configured to work with OOTDiffusion running on EC2.

## API Configuration

- **Server URL**: `http://100.25.110.185:7860/`
- **Mode**: Half-body (`/process_hd`)
- **Parameters**: 6 required parameters

## Request Structure

```kotlin
data = listOf(
    bodyFileData,          // 1. Model image (FileData dict)
    clothingFileData,      // 2. Garment image (FileData dict)
    1,                     // 3. Images (number of outputs: 1-4)
    20,                    // 4. Steps (denoising steps: 10-40)
    2.0,                   // 5. Guidance scale (1.0-5.0)
    42                     // 6. Seed (-1 for random, or specific seed)
)
```

## Available Parameters (Customizable)

You can adjust these in `TryOnViewModel.kt` line 274-277:

| Parameter | Current Value | Range | Description |
|-----------|---------------|-------|-------------|
| Images | 1 | 1-4 | Number of output images |
| Steps | 20 | 10-40 | Denoising steps (higher = better quality, slower) |
| Guidance scale | 2.0 | 1.0-5.0 | How closely to follow the prompt |
| Seed | 42 | -1 to 2147483647 | Random seed (-1 for random) |

## Two Available Endpoints

Your OOTDiffusion server has two modes:

### 1. Half-body mode (`/process_hd`) - **Currently Active**
- **Function Index**: 0
- **Parameters**: 6 (Model, Garment, Images, Steps, Guidance, Seed)
- **Best for**: Upper-body garments (t-shirts, jackets, tops)

### 2. Dress code mode (`/process_dc`)
- **Function Index**: 1
- **Parameters**: 7 (Model, Garment, **Category**, Images, Steps, Guidance, Seed)
- **Category options**: "Upper-body", "Lower-body", "Dress"
- **Best for**: Full outfits and dresses

## How to Switch Modes

To use Dress code mode instead, update `TryOnViewModel.kt` line 270-281:

```kotlin
val request = GradioRequest(
    data = listOf(
        bodyFileData,          // 1. Model image
        clothingFileData,      // 2. Garment image
        "Upper-body",          // 3. Category (NEW!)
        1,                     // 4. Images
        20,                    // 5. Steps
        2.0,                   // 6. Guidance scale
        42                     // 7. Seed
    ),
    fn_index = 1,  // Change to 1 for /process_dc
    session_hash = sessionHash
)
```

## Testing Your App

1. **Build and Run** in Android Studio
2. **Upload** a body photo
3. **Upload** a garment photo
4. **Click** "Generate Try-On"
5. **Wait** 1-3 minutes for processing
6. **View** the result!

## Expected Processing Time

- **GPU (EC2 g4dn.xlarge)**: 30-90 seconds
- **Steps = 20**: ~1 minute
- **Steps = 40**: ~2-3 minutes

## Troubleshooting

### Error: "Empty response from server"
✅ **FIXED!** - Now sending all 6 required parameters

### Error: "OOTDiffusion failed: ..."
- Check that both images are valid JPG/PNG
- Check EC2 server logs: `ssh ubuntu@100.25.110.185 "tail -f ootdiffusion.log"`
- Verify EC2 instance is running and not out of memory

### No result image
- Check Logcat for: `"Found result image URL"`
- Verify the API is returning data in Gallery format

## Changes Made

### 1. `TryOnViewModel.kt` (Lines 251-281)
- Added 4 additional parameters (Images, Steps, Guidance, Seed)
- Set default values optimized for quality vs speed

### 2. `TryOnViewModel.kt` (Lines 417-475)
- Updated SSE parser to handle `process_completed` message
- Added Gallery format image extraction
- Improved error handling for `success: false` responses

### 3. `HomeScreen.kt` (Lines 237-282)
- Simplified UI to show 3 sample categories
- Updated loading message for OOTDiffusion

## API Information Source

Retrieved from: `http://100.25.110.185:7860/info`

Full API documentation available at: `http://100.25.110.185:7860/?view=api`
