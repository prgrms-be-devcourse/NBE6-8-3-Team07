"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { customFetch } from "@/utils/customFetch";

interface Fairytale {
  id: number;
  title: string;
  createdAt: string;
  // 백엔드에서 제공될 수 있는 추가 필드들
  childName?: string;
  childRole?: string;
  characters?: string;
  place?: string;
  mood?: string;
  lesson?: string;
  // 프론트엔드에서 관리할 좋아요 상태
  likeCount?: number;
  isLiked?: boolean;
  isPublic?: boolean;
}

interface PageInfo {
  content: Fairytale[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

export default function FairytaleGallery() {
  const [fairytales, setFairytales] = useState<Fairytale[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [likedFairytales, setLikedFairytales] = useState<Set<number>>(
    new Set()
  );
  const [currentPage, setCurrentPage] = useState(0);
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [isSearchMode, setIsSearchMode] = useState(false);
  const router = useRouter();
  const NEXT_PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

  useEffect(() => {
    fetchFairytales(currentPage);
  }, [currentPage]);

  useEffect(() => {
    console.log("fetchLikedFairytales 호출됨");
    fetchLikedFairytales();
  }, []);

  const fetchFairytales = async (page: number = 0) => {
    try {
      setIsLoading(true);
      const response = await customFetch(
        `${NEXT_PUBLIC_API_BASE_URL}/fairytales/gallery?page=${page}&size=6`,
        {
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();

      setFairytales(data.content);
      setPageInfo({
        content: data.content,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        currentPage: data.number,
        size: data.size,
      });
      setError(null);
    } catch (err) {
      console.error("Error fetching fairytales:", err);
      // 모든 에러에 대해 "아직 공개된 동화가 없습니다" 메시지 표시
      setError("아직 공개된 동화가 없습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const fetchLikedFairytales = async () => {
    try {
      console.log("좋아요 목록 조회 시작");
      const response = await customFetch(`${NEXT_PUBLIC_API_BASE_URL}/likes`, {
        credentials: "include",
      });

      console.log("좋아요 목록 응답:", response.status, response.statusText);

      if (response.ok) {
        const likes = await response.json();
        console.log("좋아요 목록 데이터:", likes);
        const likedIds = new Set<number>(
          likes.map((like: { fairytaleId: number }) => Number(like.fairytaleId))
        );
        setLikedFairytales(likedIds);
        console.log("좋아요 ID 목록:", Array.from(likedIds));
      }
    } catch (error) {
      console.error("좋아요 목록 조회 실패:", error);
    }
  };

  const handleSearch = async (page: number = 0, keyword?: string) => {
    const searchTerm = keyword || searchKeyword;
    
    // 2글자 미만이면 함수 실행하지 않음
    if (searchTerm.trim().length < 2) {
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      setIsSearchMode(true);

      const response = await customFetch(
        `${NEXT_PUBLIC_API_BASE_URL}/api/fairytales/search?keyword=${encodeURIComponent(searchTerm.trim())}&page=${page}&size=6`,
        {
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();

      setFairytales(data.content);
      setPageInfo({
        content: data.content,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        currentPage: data.number,
        size: data.size,
      });
      setCurrentPage(page);
    } catch (err) {
      console.error("Error searching fairytales:", err);
      setError("검색 중 오류가 발생했습니다.");
      setFairytales([]);
      setPageInfo(null);
    } finally {
      setIsLoading(false);
    }
  };

  const clearSearch = () => {
    setSearchKeyword("");
    setIsSearchMode(false);
    setCurrentPage(0);
    fetchFairytales(0);
  };

  const handleSearchKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && searchKeyword.trim().length >= 2) {
      setCurrentPage(0);
      handleSearch(0);
    }
  };

  const toggleLike = async (fairytaleId: number) => {
    try {
      const isCurrentlyLiked = likedFairytales.has(fairytaleId);

      const response = await customFetch(
        `${NEXT_PUBLIC_API_BASE_URL}/like/${fairytaleId}`,
        {
          method: isCurrentlyLiked ? "DELETE" : "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
        }
      );

      if (response.ok) {
        const newLikedFairytales = new Set(likedFairytales);
        if (isCurrentlyLiked) {
          newLikedFairytales.delete(fairytaleId);
        } else {
          newLikedFairytales.add(fairytaleId);
        }
        setLikedFairytales(newLikedFairytales);
      } else {
        const errorText = await response.text();
        console.error(
          "좋아요 처리 실패:",
          response.status,
          response.statusText,
          errorText
        );
      }
    } catch (error) {
      console.error("좋아요 처리 중 오류:", error);
    }
  };

  const handleFairytaleClick = (fairytaleId: number) => {
    router.push(`/fairytale/gallery/${fairytaleId}`);
  };

  const handlePageChange = (page: number) => {
    if (isSearchMode) {
      handleSearch(page);
    } else {
      setCurrentPage(page);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  const renderPagination = () => {
    if (!pageInfo || pageInfo.totalPages <= 1) return null;

    const pages = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(
      0,
      pageInfo.currentPage - Math.floor(maxVisiblePages / 2)
    );
    const endPage = Math.min(
      pageInfo.totalPages - 1,
      startPage + maxVisiblePages - 1
    );

    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }

    // 이전 페이지 버튼
    if (pageInfo.currentPage > 0) {
      pages.push(
        <button
          key="prev"
          onClick={() => handlePageChange(pageInfo.currentPage - 1)}
          className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-l-md hover:bg-gray-50"
        >
          이전
        </button>
      );
    }

    // 페이지 번호들
    for (let i = startPage; i <= endPage; i++) {
      pages.push(
        <button
          key={i}
          onClick={() => handlePageChange(i)}
          className={`px-3 py-2 text-sm font-medium ${
            i === pageInfo.currentPage
              ? "text-orange-600 bg-orange-50 border-orange-300"
              : "text-gray-500 bg-white border-gray-300 hover:bg-gray-50"
          } border`}
        >
          {i + 1}
        </button>
      );
    }

    // 다음 페이지 버튼
    if (pageInfo.currentPage < pageInfo.totalPages - 1) {
      pages.push(
        <button
          key="next"
          onClick={() => handlePageChange(pageInfo.currentPage + 1)}
          className="px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-r-md hover:bg-gray-50"
        >
          다음
        </button>
      );
    }

    return (
      <div className="flex justify-center mt-8">
        <div className="flex space-x-0">{pages}</div>
      </div>
    );
  };

  if (isLoading) {
    return (
      <main className="py-12 space-y-14 text-gray-800">
        <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
          <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
            <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
              동화 갤러리
            </h1>
          </div>
        </section>

        <section className="max-w-5xl mx-auto px-6">
          <div className="flex justify-center items-center h-64">
            <div className="text-2xl text-gray-600 animate-pulse">
              동화를 불러오는 중...
            </div>
          </div>
        </section>
      </main>
    );
  }

  if (error) {
    return (
      <main className="py-12 space-y-14 text-gray-800">
        <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
          <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
            <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
              동화 갤러리
            </h1>
          </div>
        </section>

        <section className="max-w-5xl mx-auto px-6">
          <div className="flex justify-center items-center h-64">
            <div className="text-2xl text-red-600">❌ {error}</div>
          </div>
        </section>
      </main>
    );
  }

  if (fairytales.length === 0) {
    return (
      <main className="py-12 space-y-14 text-gray-800">
        <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
          <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
            <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
              동화 갤러리
            </h1>
          </div>
        </section>

        <section className="max-w-5xl mx-auto px-6">
          <div className="flex justify-center items-center h-64">
            <div className="text-2xl text-gray-600">
              {isSearchMode ? "🔍 검색 결과가 없습니다" : "📚 아직 등록된 동화가 없습니다."}
            </div>
          </div>
        </section>

        {/* 검색 바 - 검색 모드일 때만 표시 */}
        {isSearchMode && (
          <section className="max-w-5xl mx-auto px-6 py-8">
            <div className="flex justify-center">
              <div className="max-w-md relative w-full">
                <input
                  type="text"
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  onKeyPress={handleSearchKeyPress}
                  placeholder="동화를 검색해보세요 (2글자 이상)"
                  className="w-full px-4 py-2 text-lg border-2 border-orange-300 rounded-lg focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 bg-white shadow-md"
                />
                <div className="absolute right-2 top-1/2 transform -translate-y-1/2 flex space-x-1">
                  <button
                    onClick={clearSearch}
                    className="bg-gray-500 text-white px-3 py-1 rounded-md hover:bg-gray-600 transition-colors text-sm cursor-pointer"
                  >
                    초기화
                  </button>
                  <button
                    onClick={() => {
                      setCurrentPage(0);
                      handleSearch(0);
                    }}
                    disabled={isLoading || searchKeyword.trim().length < 2}
                    className="bg-orange-500 text-white px-3 py-1 rounded-md hover:bg-orange-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors text-sm cursor-pointer"
                  >
                    {isLoading ? "검색중..." : "검색"}
                  </button>
                </div>
              </div>
            </div>
          </section>
        )}
      </main>
    );
  }

  return (
    <main className="py-12 space-y-14 text-gray-800">
      <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
        <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
          <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
            동화 갤러리
          </h1>
        </div>
      </section>


      <section className="max-w-5xl mx-auto px-6">
        <div className="text-center mb-12">
          {isSearchMode ? (
            <>
              <h2 className="text-3xl font-bold text-orange-400 mb-4">
                검색 결과
              </h2>
              <p className="text-lg text-gray-600">
                <span className="font-bold text-orange-600">"{searchKeyword.trim()}"</span>에 대한 검색 결과입니다
              </p>
            </>
          ) : (
            <>
              <h2 className="text-3xl font-bold text-orange-400 mb-4">
                우리 아이들의 특별한 이야기들
              </h2>
              <p className="text-lg text-gray-600">
                아이와 함께 만든 소중한 동화들을 모아봤어요
              </p>
            </>
          )}
          {pageInfo && (
            <p className="text-sm text-gray-500 mt-2">
              총 {pageInfo.totalElements}개의 동화 중 {pageInfo.currentPage + 1}
              페이지
            </p>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {fairytales.map((fairytale) => (
            <div
              key={fairytale.id}
              className="group bg-white rounded-lg shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2 cursor-pointer overflow-hidden border border-orange-200"
              onClick={() => handleFairytaleClick(fairytale.id)}
            >
              {/* 상단 오렌지 바 */}
              <div className="h-2 bg-orange-400"></div>

              <div className="p-6">
                {/* 제목 영역 */}
                <div className="mb-4">
                  <h3 className="text-xl font-bold text-gray-800 line-clamp-2 group-hover:text-orange-600 transition-colors duration-300 leading-tight">
                    {fairytale.title}
                  </h3>
                </div>

                {/* 카드 내용 */}
                <div className="mb-6">
                  {/* 동화 내용 미리보기 */}
                  <div className="mb-4">
                    <p className="text-sm text-gray-600 line-clamp-3 leading-relaxed">
                      아이와 함께 만든 특별한 동화입니다. 자세한 내용을 보려면
                      카드를 클릭해주세요!
                    </p>
                  </div>

                  {/* 동적 데이터 표시 (백엔드에서 제공될 때) */}
                  {(fairytale.characters ||
                    fairytale.place ||
                    fairytale.mood) && (
                    <div className="space-y-3 mb-4">
                      {fairytale.characters && (
                        <div className="flex items-center space-x-2">
                          <span className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded-full font-medium">
                            👥 등장인물
                          </span>
                          <span className="text-sm text-gray-700">
                            {fairytale.characters}
                          </span>
                        </div>
                      )}
                      {fairytale.place && (
                        <div className="flex items-center space-x-2">
                          <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded-full font-medium">
                            🏰 장소
                          </span>
                          <span className="text-sm text-gray-700">
                            {fairytale.place}
                          </span>
                        </div>
                      )}
                      {fairytale.mood && (
                        <div className="flex items-center space-x-2">
                          <span className="text-xs bg-orange-100 text-orange-700 px-2 py-1 rounded-full font-medium">
                            ✨ 분위기
                          </span>
                          <span className="text-sm text-gray-700">
                            {fairytale.mood}
                          </span>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* 하단 영역 */}
                <div className="pt-4 border-t border-orange-200">
                  <div className="flex justify-between items-center">
                    <div className="flex items-center text-sm text-gray-500">
                      <span className="mr-2">📅</span>
                      <span>{formatDate(fairytale.createdAt)}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleLike(fairytale.id);
                        }}
                        className={`p-2 rounded-full transition-all duration-300 transform hover:scale-110 cursor-pointer ${
                          likedFairytales.has(fairytale.id)
                            ? "text-red-500 hover:text-red-600 bg-red-50"
                            : "text-gray-400 hover:text-red-500 hover:bg-red-50"
                        }`}
                      >
                        <svg
                          className="w-6 h-6"
                          fill={
                            likedFairytales.has(fairytale.id)
                              ? "currentColor"
                              : "none"
                          }
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                          />
                        </svg>
                      </button>
                      {fairytale.likeCount !== undefined && (
                        <span className="text-sm text-gray-600 font-medium">
                          {fairytale.likeCount}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* 페이징 컴포넌트 */}
        {renderPagination()}
      </section>

      {/* 검색 바 - 하단 */}
      <section className="max-w-5xl mx-auto px-6 py-8">
        <div className="flex justify-center">
          <div className="max-w-md relative w-full">
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyPress={handleSearchKeyPress}
              placeholder="동화를 검색해보세요 (2글자 이상)"
              className="w-full px-4 py-2 text-lg border-2 border-orange-300 rounded-lg focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 bg-white shadow-md"
            />
            <div className="absolute right-2 top-1/2 transform -translate-y-1/2 flex space-x-1">
              {isSearchMode && (
                <button
                  onClick={clearSearch}
                  className="bg-gray-500 text-white px-3 py-1 rounded-md hover:bg-gray-600 transition-colors text-sm cursor-pointer"
                >
                  초기화
                </button>
              )}
              <button
                onClick={() => {
                  setCurrentPage(0);
                  handleSearch(0);
                }}
                disabled={isLoading || searchKeyword.trim().length < 2}
                className="bg-orange-500 text-white px-3 py-1 rounded-md hover:bg-orange-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors text-sm cursor-pointer"
              >
                {isLoading ? "검색중..." : "검색"}
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
