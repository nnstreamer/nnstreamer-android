## Configuration files of ML model

### mobilenet_v1_1.0_224_quant

### yolov8s_float32

### llamacpp

To run llamacpp model, copy gguf file into this directory.
You can download small size LLM gguf model [here](https://huggingface.co/TheBloke/rocket-3B-GGUF).
To enable optimized GEMM/GEMV kernels use Q4_0 to Q4_0_x_x from [prebuilt libraries](https://github.com/nnstreamer/nnstreamer-android-resource).

### llama2c

To run llama2c model, copy model.bin and tokenizer.bin file into this directory.
You can download and train custom small sized model that can be executed on Android devices [here](https://github.com/karpathy/llama2.c/tree/master?tab=readme-ov-file#custom-tokenizers)
