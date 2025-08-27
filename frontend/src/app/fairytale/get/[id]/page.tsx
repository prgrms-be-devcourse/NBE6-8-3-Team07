'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { Fairytale } from '@/context/fairytaleContext';
import { customFetch } from '@/utils/customFetch';
import { FaLock, FaGlobe, FaPen, FaEdit, FaTrash } from 'react-icons/fa';

interface GroupedKeywords {
  [key: string]: string[];
}

interface ThoughtsData {
  id?: number;
  name: string;          
  content: string; 
  parentContent: string;
  fairytaleId: number;
}

interface ThoughtsResponse {
  id: number;
  name: string;
  content: string;
  parentContent: string;
  fairytaleId: number;
  createdAt: string;
  updatedAt: string;
}

const FairytaleReader = () => {
  const params = useParams();
  const fairytaleId = params.id as string;

  const [isKeywordPopupOpen, setIsKeywordPopupOpen] = useState(false);
  const [isThoughtsPopupOpen, setIsThoughtsPopupOpen] = useState(false);
  const [fairytale, setFairytale] = useState<Fairytale | null>(null);
  const [groupedKeywords, setGroupedKeywords] = useState<GroupedKeywords>({
    CHILD_NAME: [],
    CHILD_ROLE: [],
    CHARACTERS: [],
    PLACE: [],
    MOOD: [],
    LESSON: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpdatingVisibility, setIsUpdatingVisibility] = useState(false);
  
  // 아이 생각 관련 상태
  const [thoughts, setThoughts] = useState<ThoughtsResponse | null>(null);
  const [thoughtsForm, setThoughtsForm] = useState<ThoughtsData>({
    name: '',
    content: '',
    parentContent: '',
    fairytaleId: Number(fairytaleId),
  });
  const [isSubmittingThoughts, setIsSubmittingThoughts] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);

  const keywordCategoryNames: { [key: string]: string } = {
    CHILD_NAME: '주인공 이름',
    CHILD_ROLE: '주인공 역할',
    CHARACTERS: '등장인물',
    PLACE: '장소',
    MOOD: '분위기',
    LESSON: '교훈',
  };
  
  // 공개설정 토글 메서드
  const handleToggleVisibility = async () => {
    if (!fairytale) return;
    
    setIsUpdatingVisibility(true);
    
    try {
      const response = await customFetch(
        `https://nbe6-8-2-team07.onrender.com/fairytales/${fairytaleId}/visibility?isPublic=${!fairytale.isPublic}`,
        {
          method: 'PATCH',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (!response.ok) {
        throw new Error(`공개 설정 변경 실패! status: ${response.status}`);
      }

      setFairytale(prev => 
        prev ? { ...prev, isPublic: !prev.isPublic } : null
      );

      alert(`동화가 ${!fairytale.isPublic ? '공개' : '비공개'}로 설정되었습니다.`);
      
    } catch (error: unknown) {
      console.error('공개 설정 변경 중 오류 발생:', error);
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.';
      alert(`공개 설정 변경 중 오류가 발생했습니다: ${errorMessage}`);
    } finally {
      setIsUpdatingVisibility(false);
    }
  };

  // 아이 생각 조회
  const fetchThoughts = useCallback(async () => {
    try {
      const response = await customFetch(`https://nbe6-8-2-team07.onrender.com/api/thoughts/fairytale/${fairytaleId}`, {
        credentials: 'include',
      });
      
      if (response.ok) {
        const thoughtsData: ThoughtsResponse = await response.json();
        setThoughts(thoughtsData);
        setThoughtsForm({
          name: thoughtsData.name,
          content: thoughtsData.content,
          parentContent: thoughtsData.parentContent,
          fairytaleId: thoughtsData.fairytaleId,
        });
      }
    } catch {
      console.log('아이 생각이 아직 없습니다.');
    }
  }, [fairytaleId]);

  // 아이 생각 저장/수정
  const handleSaveThoughts = async () => {
    if (!thoughtsForm.name.trim() || !thoughtsForm.content.trim() || !thoughtsForm.parentContent.trim()) {
      alert('아이 이름, 아이 생각, 부모 생각을 모두 입력해주세요.');
      return;
    }

    setIsSubmittingThoughts(true);

    try {
      const url = thoughts 
        ? `https://nbe6-8-2-team07.onrender.com/api/thoughts/${thoughts.id}`
        : 'https://nbe6-8-2-team07.onrender.com/api/thoughts';
      
      const method = thoughts ? 'PUT' : 'POST';
      
      console.log('전송할 데이터:', thoughtsForm); // 디버깅용
      
      const response = await customFetch(url, {
        method,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(thoughtsForm),
      });

      console.log('응답 상태:', response.status); // 디버깅용

      if (!response.ok) {
        const errorText = await response.text();
        console.error('에러 응답:', errorText);
        throw new Error(`아이 생각 저장 실패! status: ${response.status}, message: ${errorText}`);
      }

      const savedThoughts: ThoughtsResponse = await response.json();
      setThoughts(savedThoughts);
      setIsEditMode(false);
      alert(thoughts ? '아이 생각이 수정되었습니다.' : '아이 생각이 저장되었습니다.');
      
    } catch (error: unknown) {
      console.error('아이 생각 저장 중 오류 발생:', error);
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.';
      alert(`아이 생각 저장 중 오류가 발생했습니다: ${errorMessage}`);
    } finally {
      setIsSubmittingThoughts(false);
    }
  };

  // 아이 생각 삭제
  const handleDeleteThoughts = async () => {
    if (!thoughts) return;
    
    if (!confirm('정말로 아이 생각을 삭제하시겠습니까?')) return;

    try {
      const response = await customFetch(`https://nbe6-8-2-team07.onrender.com/api/thoughts/${thoughts.id}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      if (!response.ok) {
        throw new Error(`아이 생각 삭제 실패! status: ${response.status}`);
      }

      setThoughts(null);
      setThoughtsForm({
        name: '',
        content: '',
        parentContent: '',
        fairytaleId: Number(fairytaleId),
      });
      setIsEditMode(false);
      alert('아이 생각이 삭제되었습니다.');
      
    } catch (error: unknown) {
      console.error('아이 생각 삭제 중 오류 발생:', error);
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.';
      alert(`아이 생각 삭제 중 오류가 발생했습니다: ${errorMessage}`);
    }
  };

  // 아이 생각 팝업 열기
  const handleOpenThoughtsPopup = () => {
    setIsThoughtsPopupOpen(true);
    if (!thoughts) {
      setIsEditMode(true);
    }
  };

  // 아이 생각 팝업 닫기
  const handleCloseThoughtsPopup = () => {
    setIsThoughtsPopupOpen(false);
    setIsEditMode(false);
    if (thoughts) {
      setThoughtsForm({
        id: thoughts.id,
        name: thoughts.name,
        content: thoughts.content,
        parentContent: thoughts.parentContent,
        fairytaleId: thoughts.fairytaleId,
      });
    }
  };

  useEffect(() => {
    const fetchFairytaleData = async () => {
      if (!fairytaleId) {
        setError('Fairytale ID is missing.');
        setLoading(false);
        return;
      }

      try {
        // Fetch fairytale details
        const fairytaleResponse = await customFetch(`https://nbe6-8-2-team07.onrender.com/fairytales/${fairytaleId}`,{
          credentials: 'include',
        });
        if (!fairytaleResponse.ok) {
          throw new Error(`Failed to fetch fairytale: ${fairytaleResponse.statusText}`);
        }
        const fairytaleData: Fairytale = await fairytaleResponse.json();
        setFairytale(fairytaleData);

        const parseKeywords = (str?: string) =>
          str ? str.split(',').map(s => s.trim()).filter(s => s.length > 0) : [];

        const newGroupedKeywords: GroupedKeywords = {
          CHILD_NAME: parseKeywords((fairytaleData as Fairytale & { childName?: string }).childName),
          CHILD_ROLE: parseKeywords((fairytaleData as Fairytale & { childRole?: string }).childRole),
          CHARACTERS: parseKeywords((fairytaleData as Fairytale & { characters?: string }).characters),
          PLACE: parseKeywords((fairytaleData as Fairytale & { place?: string }).place),
          MOOD: parseKeywords((fairytaleData as Fairytale & { mood?: string }).mood),
          LESSON: parseKeywords((fairytaleData as Fairytale & { lesson?: string }).lesson),
        };

        setGroupedKeywords(newGroupedKeywords);

        // 아이 생각 조회
        await fetchThoughts();

      } catch (e: unknown) {
        const errorMessage = e instanceof Error ? e.message : '알 수 없는 오류가 발생했습니다.';
        setError(errorMessage);
      } finally {
        setLoading(false);
      }
    };

    fetchFairytaleData();
  }, [fairytaleId, fetchThoughts]);

  if (loading) return <div className="container mx-auto p-4">로딩 중...</div>;
  if (error) return <div className="container mx-auto p-4 text-red-500">에러: {error}</div>;
  if (!fairytale) return <div className="container mx-auto p-4">동화를 찾을 수 없습니다.</div>;

  return (
    <div className="container mx-auto p-4 mb-6 bg-[#FAF9F6] min-h-screen flex flex-col items-center relative">
      <div className="relative w-full max-w-3xl">
        <div className="bg-white shadow-lg rounded-lg mt-8">
          <div className="p-8">
            <div className="flex items-center justify-between mb-6">
              <h1 className="text-3xl font-bold text-gray-800 flex-1 mr-4">{fairytale.title}</h1>
            
              <div className="flex items-center">
                <button
                  onClick={handleToggleVisibility}
                  disabled={isUpdatingVisibility}
                  className={`flex items-center px-4 py-2 rounded-full transition-all duration-300 ${
                    isUpdatingVisibility
                      ? 'bg-gray-200 text-gray-500 cursor-not-allowed'
                      : fairytale.isPublic
                        ? 'bg-green-100 text-green-800 hover:bg-green-200 cursor-pointer'
                        : 'bg-gray-100 text-gray-800 hover:bg-gray-200 cursor-pointer'
                  }`}
                  title={
                    isUpdatingVisibility
                      ? '설정 변경 중...'
                      : `현재 ${fairytale.isPublic ? '공개' : '비공개'} 상태 (클릭하여 변경)`
                  }
                >
                  {isUpdatingVisibility ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-500 mr-2"></div>
                      <span className="text-sm font-medium">변경 중...</span>
                    </>
                  ) : (
                    <>
                      {fairytale.isPublic ? (
                        <>
                          <FaGlobe className="mr-2" />
                          <span className="text-sm font-medium">공개</span>
                        </>
                      ) : (
                        <>
                          <FaLock className="mr-2" />
                          <span className="text-sm font-medium">비공개</span>
                        </>
                      )}
                    </>
                  )}
                </button>
              </div>
            </div>

            {fairytale.imageUrl && (
              <div className="flex justify-center mb-6">
                <div className="w-90 h-90 bg-gray-50 rounded-lg overflow-hidden shadow-sm">
                  <img 
                    src={fairytale.imageUrl} 
                    alt={fairytale.title}
                    className="w-full h-full object-contain"
                  />
                </div>
              </div>
            )}
            
            <div className="text-lg leading-relaxed text-gray-700 mb-8">
              <p style={{ whiteSpace: 'pre-line' }}>{fairytale.content}</p>
            </div>
          </div>
        </div>
        
        {/* 플로팅 버튼들 */}
        <div className="fixed bottom-8 right-8 flex flex-col gap-3">
          <button
            onClick={() => setIsKeywordPopupOpen(true)}
            className="bg-orange-400 text-white text-lg px-4 py-2 rounded-full shadow-lg hover:bg-orange-500 transition-colors cursor-pointer"
          >
            키워드 보기
          </button>
          
          <button
            onClick={handleOpenThoughtsPopup}
            className="bg-blue-500 text-white text-lg px-4 py-2 rounded-full shadow-lg hover:bg-blue-600 transition-colors cursor-pointer flex items-center gap-2"
          >
            <FaPen size={16} />
            {thoughts ? '아이 생각 보기' : '아이 생각 기록'}
          </button>
        </div>
      </div>

      {/* 키워드 팝업 */}
      {isKeywordPopupOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-30 flex justify-center items-center z-50">
          <div className="bg-white p-8 rounded-lg shadow-2xl max-w-lg w-full overflow-auto max-h-[80vh]">
            <h2 className="text-2xl font-bold mb-4">동화 키워드</h2>
            <div className="space-y-4">
              {Object.entries(groupedKeywords).map(([type, keywords]) => (
                <div key={type}>
                  <h3 className="font-semibold text-orange-600 text-lg mb-2">{keywordCategoryNames[type] || type}</h3>
                  <div className="flex flex-wrap gap-2">
                    {keywords.length > 0 ? (
                      keywords.map((keyword, index) => (
                        <span
                          key={index}
                          className="bg-orange-400 text-white px-2 py-1 rounded-full text-sm"
                        >
                          {keyword}
                        </span>
                      ))
                    ) : (
                      <span className="text-gray-400">등록된 키워드가 없습니다.</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <div className="flex justify-end mt-6">
              <button
                onClick={() => setIsKeywordPopupOpen(false)}
                className="bg-orange-500 text-white px-4 py-2 rounded-lg hover:bg-orange-600 transition-colors cursor-pointer"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 아이 생각 팝업 */}
      {isThoughtsPopupOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-30 flex justify-center items-center z-50">
          <div className="bg-white p-8 rounded-lg shadow-2xl max-w-2xl w-full overflow-auto max-h-[80vh]">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold">아이 생각 기록</h2>
              {thoughts && !isEditMode && (
                <div className="flex gap-2">
                  <button
                    onClick={() => setIsEditMode(true)}
                    className="bg-blue-500 text-white px-3 py-1 rounded-lg hover:bg-blue-600 transition-colors flex items-center gap-2 cursor-pointer"
                  >
                    <FaEdit size={14} />
                    수정
                  </button>
                  <button
                    onClick={handleDeleteThoughts}
                    className="bg-red-500 text-white px-3 py-1 rounded-lg hover:bg-red-600 transition-colors flex items-center gap-2 cursor-pointer"
                  >
                    <FaTrash size={14} />
                    삭제
                  </button>
                </div>
              )}
            </div>

            <div className="space-y-6">
              {/* 아이 이름 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  아이 이름 *
                </label>
                {isEditMode ? (
                  <input
                    type="text"
                    value={thoughtsForm.name}
                    onChange={(e) => setThoughtsForm(prev => ({ ...prev, name: e.target.value }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="아이 이름을 입력하세요"
                  />
                ) : (
                  <p className="px-3 py-2 bg-gray-50 rounded-lg">{thoughts?.name || '입력된 이름이 없습니다.'}</p>
                )}
              </div>

              {/* 아이 생각 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  아이 생각 *
                </label>
                {isEditMode ? (
                  <textarea
                    value={thoughtsForm.content}
                    onChange={(e) => setThoughtsForm(prev => ({ ...prev, content: e.target.value }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 h-32 resize-none"
                    placeholder="아이가 이 동화에 대해 어떻게 생각하는지 적어주세요"
                  />
                ) : (
                  <p className="px-3 py-2 bg-gray-50 rounded-lg min-h-[100px] whitespace-pre-line">
                    {thoughts?.content || '입력된 생각이 없습니다.'}
                  </p>
                )}
              </div>

              {/* 부모 생각 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  부모 생각 *
                </label>
                {isEditMode ? (
                  <textarea
                    value={thoughtsForm.parentContent}
                    onChange={(e) => setThoughtsForm(prev => ({ ...prev, parentContent: e.target.value }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 h-32 resize-none"
                    placeholder="부모님의 생각이나 느낀 점을 적어주세요"
                  />
                ) : (
                  <p className="px-3 py-2 bg-gray-50 rounded-lg min-h-[100px] whitespace-pre-line">
                    {thoughts?.parentContent || '입력된 생각이 없습니다.'}
                  </p>
                )}
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              {isEditMode && (
                <button
                  onClick={handleSaveThoughts}
                  disabled={isSubmittingThoughts}
                  className="bg-blue-500 text-white px-6 py-2 rounded-lg hover:bg-blue-600 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed cursor-pointer"
                >
                  {isSubmittingThoughts ? '저장 중...' : thoughts ? '수정' : '저장'}
                </button>
              )}
              <button
                onClick={handleCloseThoughtsPopup}
                className="bg-gray-500 text-white px-6 py-2 rounded-lg hover:bg-gray-600 transition-colors cursor-pointer"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FairytaleReader;