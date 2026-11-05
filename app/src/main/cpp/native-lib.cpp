#include <jni.h>
#include <array>
#include <cmath>
#include <algorithm>

namespace {
class OneEuro {
public:
    explicit OneEuro(float min_cutoff = 1.2f, float beta = 0.035f, float d_cutoff = 1.0f)
        : min_cutoff_(min_cutoff), beta_(beta), d_cutoff_(d_cutoff) {}

    float filter(float value, float dt) {
        dt = std::clamp(dt, 0.001f, 0.1f);
        if (!initialized_) {
            initialized_ = true;
            last_raw_ = value;
            filtered_ = value;
            derivative_filtered_ = 0.0f;
            return value;
        }

        const float derivative = (value - last_raw_) / dt;
        const float derivative_alpha = alpha(dt, d_cutoff_);
        derivative_filtered_ = lowpass(derivative, derivative_filtered_, derivative_alpha);

        const float cutoff = min_cutoff_ + beta_ * std::fabs(derivative_filtered_);
        const float signal_alpha = alpha(dt, cutoff);
        filtered_ = lowpass(value, filtered_, signal_alpha);
        last_raw_ = value;
        return filtered_;
    }

    void reset() {
        initialized_ = false;
        last_raw_ = 0.0f;
        filtered_ = 0.0f;
        derivative_filtered_ = 0.0f;
    }

private:
    static float lowpass(float value, float previous, float a) {
        return a * value + (1.0f - a) * previous;
    }

    static float alpha(float dt, float cutoff) {
        const float tau = 1.0f / (2.0f * static_cast<float>(M_PI) * cutoff);
        return 1.0f / (1.0f + tau / dt);
    }

    float min_cutoff_;
    float beta_;
    float d_cutoff_;
    float last_raw_ = 0.0f;
    float filtered_ = 0.0f;
    float derivative_filtered_ = 0.0f;
    bool initialized_ = false;
};

std::array<OneEuro, 3> filters;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_boostfromdr_mobile_NativeFilter_process(
    JNIEnv* env, jobject /* thiz */,
    jfloat x, jfloat y, jfloat z, jfloat delta_seconds) {

    const float dt = std::clamp(delta_seconds, 0.001f, 0.1f);
    const std::array<float, 3> input{x, y, z};
    jfloat output[3];

    for (int i = 0; i < 3; ++i) {
        output[i] = filters[i].filter(input[i], dt);
    }

    jfloatArray result = env->NewFloatArray(3);
    if (result != nullptr) {
        env->SetFloatArrayRegion(result, 0, 3, output);
    }
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_boostfromdr_mobile_NativeFilter_reset(JNIEnv* /* env */, jobject /* thiz */) {
    for (auto& filter : filters) {
        filter.reset();
    }
}
