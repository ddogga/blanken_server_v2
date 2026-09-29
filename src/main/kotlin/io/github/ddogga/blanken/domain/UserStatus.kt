package io.github.ddogga.blanken.domain

enum class UserStatus(val description: String) {

    ACTIVE("활동중인 계정"),
    PENDING("유저 정보 없이 소셜 계정으로만 가입된 상태. 추후 추가 유저 정보 입력 받은 후 ACTIVE로 변경"),
    WITHDRAWN("탈퇴후 재가입 쿨타임을 위한 활동 정지 상태")
}