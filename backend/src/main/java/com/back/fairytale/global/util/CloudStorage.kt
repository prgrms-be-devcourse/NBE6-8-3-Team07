package com.back.fairytale.global.util

import org.springframework.web.multipart.MultipartFile

interface CloudStorage {
    /**
     * 이미지 파일을 클라우드에 업로드하고, 업로드된 이미지의 URL 리스트를 반환합니다.
     *
     * @param imgFiles 업로드할 이미지 파일 리스트
     * @return 업로드된 이미지의 URL 리스트
     */
    fun uploadImages(imgFiles: List<MultipartFile>): List<String>

    /**
     * 클라우드에서 이미지 파일을 삭제합니다.
     *
     * @param imageUrls 삭제할 이미지의 URL 리스트
     */
    fun deleteImages(imageUrls: List<String>)

    /**
     * 클라우드에서 기존 이미지 파일을 삭제하고, 새로운 이미지 파일을 업로드합니다.
     *
     * @param imageUrls 기존 이미지의 URL 리스트
     * @param imgFiles    새로 업로드할 이미지 파일 리스트
     */
    fun updateImages(imageUrls: List<String>, imgFiles: List<MultipartFile>)
}