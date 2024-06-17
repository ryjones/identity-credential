package com.android.identity.issuance.evidence

data class EvidenceRequestSelfieVideo(val poseSequence: List<Poses>): EvidenceRequest() {
    enum class Poses {
        FRONT,
        TILT_HEAD_UP,
        TILT_HEAD_DOWN
    }
}
