# A Single APK integration of NNStreamer and ML API

This project offers a single Android application package (APK) solution for on-device Machine
Learning Operations (MLOps). As one of the core components, this APK includes an Android Service
that uses [ML API](https://github.com/nnstreamer/api) to maintain
[NNStreamer](https://github.com/nnstreamer/nnstreamer) pipelines and Machine Learning (ML) models
for those pipelines. The Service exposes multiple end-points to address ML computation offloading
requests from external Tizen and Android devices, as well as ML task delegation requests from other
Android applications running on the same device. The APK also provides an Android Activity that
controls the Service's features. This Activity enables users to supervise the ML models and
pipelines while hosting a user interface to visualize the results of the ML computations.
In addition, sample applications are provided as concrete examples to demonstrate how to use the
Service to perform external ML computation offloading and internal delegation of ML task requests.

## Disclaimer

Note that the license of this repository, [Apache-2.0](https://spdx.org/licenses/Apache-2.0.html),
is only valid for the set of Android components provided this repository. Other software components
used in this project, such as: NNStreamer, ML API, TensorFlow Lite, etc. (especially, the files and
the cloned repositories under the ```externals``` directory), are provided as-is and are governed by
their respective licenses.
