'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Comments from '@/app/fairytale/gallery/comments';

interface FairytaleDetail {
  id: number;
  title: string;
  content: string;
  createdAt: string;
  childName?: string;
  childRole?: string;
  characters?: string;
  place?: string;
  mood?: string;
  lesson?: string;
  imageUrl?: string;
  isPublic?: boolean;
}

export default function FairytaleGalleryDetail() {
  const [fairytale, setFairytale] = useState<FairytaleDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const params = useParams();
  const router = useRouter();
  const fairytaleId = params.id;

  const fetchFairytaleDetail = useCallback(async () => {
    try {
      setIsLoading(true);
      const response = await fetch(`https://nbe6-8-2-team07.onrender.com/fairytales/gallery/${fairytaleId}`, {
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setFairytale(data);
      setError(null);
    } catch (err) {
      console.error('Error fetching fairytale detail:', err);
      setError('동화를 불러오는 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [fairytaleId]);

  useEffect(() => {
    if (fairytaleId) {
      fetchFairytaleDetail();
    }
  }, [fairytaleId, fetchFairytaleDetail]);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  if (isLoading) {
    return (
      <main className="py-12 space-y-14 text-gray-800">
        <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
          <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
            <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
              동화 상세보기
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
              동화 상세보기
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

  if (!fairytale) {
    return (
      <main className="py-12 space-y-14 text-gray-800">
        <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
          <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
            <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
              동화 상세보기
            </h1>
          </div>
        </section>

        <section className="max-w-5xl mx-auto px-6">
          <div className="flex justify-center items-center h-64">
            <div className="text-2xl text-gray-600">📚 동화를 찾을 수 없습니다.</div>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="py-12 space-y-14 text-gray-800">
      <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
        <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
          <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
            동화 상세보기
          </h1>
        </div>
      </section>

      <section className="max-w-5xl mx-auto px-6">
        <div className="bg-white rounded-lg shadow-lg overflow-hidden border border-orange-200">
          {/* 상단 오렌지 바 */}
          <div className="h-2 bg-orange-400"></div>

          <div className="p-8">
            {/* 제목 영역 */}
            <div className="mb-8">
              <h2 className="text-3xl font-bold text-gray-800 mb-4 leading-tight">
                {fairytale.title}
              </h2>
              <div className="flex items-center text-sm text-gray-500">
                <span className="mr-2">📅</span>
                <span>{formatDate(fairytale.createdAt)}</span>
              </div>
            </div>

            {/* 동화 정보 */}
            {(fairytale.childName || fairytale.childRole || fairytale.characters || fairytale.place || fairytale.mood || fairytale.lesson) && (
              <div className="mb-8 p-6 bg-orange-50 rounded-lg">
                <h3 className="text-lg font-semibold text-orange-700 mb-4">동화 정보</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {fairytale.childName && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm bg-blue-100 text-blue-700 px-3 py-1 rounded-full font-medium">
                        👶 아이 이름
                      </span>
                      <span className="text-gray-700">{fairytale.childName}</span>
                    </div>
                  )}
                  {fairytale.childRole && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm bg-green-100 text-green-700 px-3 py-1 rounded-full font-medium">
                        🎭 아이 역할
                      </span>
                      <span className="text-gray-700">{fairytale.childRole}</span>
                    </div>
                  )}
                  {fairytale.characters && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm bg-purple-100 text-purple-700 px-3 py-1 rounded-full font-medium">
                        👥 등장인물
                      </span>
                      <span className="text-gray-700">{fairytale.characters}</span>
                    </div>
                  )}
                  {fairytale.place && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm bg-yellow-100 text-yellow-700 px-3 py-1 rounded-full font-medium">
                        🏰 장소
                      </span>
                      <span className="text-gray-700">{fairytale.place}</span>
                    </div>
                  )}
                  {fairytale.mood && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm bg-pink-100 text-pink-700 px-3 py-1 rounded-full font-medium">
                        ✨ 분위기
                      </span>
                      <span className="text-gray-700">{fairytale.mood}</span>
                    </div>
                  )}
                  {fairytale.lesson && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full font-medium">
                        📚 교훈
                      </span>
                      <span className="text-gray-700">{fairytale.lesson}</span>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* 동화 내용 */}
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-gray-800 mb-4">동화 내용</h3>
              <div className="prose prose-lg max-w-none">
                <div className="text-gray-700 leading-relaxed whitespace-pre-wrap">
                  {fairytale.content}
                </div>
              </div>
            </div>

            {/* 이미지 (있는 경우) */}
            {fairytale.imageUrl && (
              <div className="mb-8">
                <h3 className="text-xl font-semibold text-gray-800 mb-4">동화 이미지</h3>
                <div className="flex justify-center">
                  <img 
                    src={fairytale.imageUrl} 
                    alt="동화 이미지" 
                    className="max-w-full h-auto rounded-lg shadow-md"
                  />
                </div>
              </div>
            )}

            {/* 댓글 섹션 */}
            {fairytale && <Comments fairytaleId={fairytale.id} />}

            {/* 하단 버튼 */}
            <div className="flex justify-center pt-6 border-t border-orange-200">
              <button
                onClick={() => router.push('/fairytale/gallery')}
                className="px-6 py-3 bg-orange-500 text-white font-semibold rounded-lg hover:bg-orange-600 transition-colors duration-300 cursor-pointer"
              >
                갤러리로 돌아가기
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
} 