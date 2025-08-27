'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { customFetch } from '@/utils/customFetch';

// 모든 슬라이드 객체의 타입을 명확하게 정의하기위한 인터페이스
interface Slide {
  title: string;
  content: string;
  image: string;
  name?: string; // 주인공 슬라이드에만 존재
  role?: string; // 주인공 슬라이드에만 존재
  addedItems?: string[]; // 키워드 슬라이드에만 존재
}

const validateName = (name: string): { isValid: boolean; message: string } => {
  if (!name.trim()) {
    return { isValid: false, message: '이름을 입력해주세요.' };
  }
  if (name.trim().length < 1 || name.trim().length > 50) {
    return { isValid: false, message: '이름은 1-50자 사이로 입력해주세요.' };
  }
  
  const trimmedName = name.trim();
  
  // 개별 자모 문자 체크 (기본 자모 + 복합 자모)
  if (/[ㄱ-ㅎㅏ-ㅣㅐㅒㅔㅖㅘㅙㅚㅝㅞㅟㅢ]/.test(trimmedName)) {
    return { isValid: false, message: '올바른 단어를 입력해주세요.' };
  }
  
  // 완성된 한글만 있는지 체크 (자모 제외)
  if (!/^[가-힣a-zA-Z0-9\s]+$/.test(trimmedName)) {
    return { isValid: false, message: '올바른 형식을 입력해주세요.' };
  }
  
  // 한글이 포함되어야 함
  if (!/[가-힣]/.test(trimmedName)) {
    return { isValid: false, message: '이름에는 한글이 포함되어야 합니다.' };
  }
  
  return { isValid: true, message: '' };
};

const validateRole = (role: string): { isValid: boolean; message: string } => {
  if (!role.trim()) {
    return { isValid: false, message: '역할을 입력해주세요.' };
  }
  if (role.trim().length < 1 || role.trim().length > 50) {
    return { isValid: false, message: '역할은 1-50자 사이로 입력해주세요.' };
  }
  
  const trimmedRole = role.trim();
  
  // 개별 자모 문자 체크 (기본 자모 + 복합 자모)
  if (/[ㄱ-ㅎㅏ-ㅣㅐㅒㅔㅖㅘㅙㅚㅝㅞㅟㅢ]/.test(trimmedRole)) {
    return { isValid: false, message: '올바른 단어를 입력해주세요.' };
  }
  
  // 완성된 한글만 있는지 체크 (자모 제외)
  if (!/^[가-힣a-zA-Z0-9\s]+$/.test(trimmedRole)) {
    return { isValid: false, message: '올바른 형식을 입력해주세요.' };
  }
  
  // 한글이 포함되어야 함
  if (!/[가-힣]/.test(trimmedRole)) {
    return { isValid: false, message: '역할에는 한글이 포함되어야 합니다.' };
  }
  
  return { isValid: true, message: '' };
};

const validateKeyword = (keyword: string): { isValid: boolean; message: string } => {
  if (!keyword.trim()) {
    return { isValid: false, message: '키워드를 입력해주세요.' };
  }
  if (keyword.trim().length < 1 || keyword.trim().length > 200) {
    return { isValid: false, message: '키워드는 1-200자 사이로 입력해주세요.' };
  }
  
  const trimmedKeyword = keyword.trim();
  
  // 개별 자모 문자 체크 (기본 자모 + 복합 자모)
  if (/[ㄱ-ㅎㅏ-ㅣㅐㅒㅔㅖㅘㅙㅚㅝㅞㅟㅢ]/.test(trimmedKeyword)) {
    return { isValid: false, message: '올바른 단어를 입력해주세요.' };
  }
  
  // 완성된 한글만 있는지 체크 (자모 제외)
  if (!/^[가-힣a-zA-Z0-9\s]+$/.test(trimmedKeyword)) {
    return { isValid: false, message: '올바른 형식을 입력해주세요.' };
  }
  
  // 한글이 포함되어야 함
  if (!/[가-힣]/.test(trimmedKeyword)) {
    return { isValid: false, message: '키워드에는 한글이 포함되어야 합니다.' };
  }
  
  return { isValid: true, message: '' };
};

export default function FairytaleCreatePage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  // useState에 Slide[] 타입을 명시적으로 적용합니다.
  const [slides, setSlides] = useState<Slide[]>([
    {
      title: "",
      content: "",
      image: "",
    },
    {
      title: "주인공",
      content: "주인공의 이름과 역할을 적어주세요.",
      image: "/images/post1.png",
      name: "",
      role: "",
    },
    {
      title: "등장인물",
      content: "동화에 등장할 사람이나 동물을 적어주세요.",
      image: "/images/post2.png",
      addedItems: [],
    },
    {
      title: "장소",
      content: "동화에 등장할 장소를 적어주세요.",
      image: "/images/post3.png",
      addedItems: [],
    },
    {
      title: "분위기",
      content: "동화의 분위기를 적어주세요.",
      image: "/images/post4.png",
      addedItems: [],
    },
    {
      title: "교훈",
      content: "동화의 교훈을 적어주세요.",
      image: "/images/post5.png",
      addedItems: [],
    },
    {
      title: "입력 내용 확인",
      content:
        "아이와 함께 고른 키워드들이 잘 들어갔는지 마지막으로 체크해볼까요?",
      image: "/images/post4.png",
    },
  ]);

  const [currentSlide, setCurrentSlide] = useState(0);
  const [currentInput, setCurrentInput] = useState('');
  const [slide1NameInput, setSlide1NameInput] = useState('');
  const [showSlide1NameInput, setShowSlide1NameInput] = useState(true);
  const [slide1RoleInput, setSlide1RoleInput] = useState('');
  const [showSlide1RoleInput, setShowSlide1RoleInput] = useState(true);
  const [nameError, setNameError] = useState('');
  const [roleError, setRoleError] = useState('');
  const [keywordError, setKeywordError] = useState('');
  const [nameSuccess, setNameSuccess] = useState('');
  const [roleSuccess, setRoleSuccess] = useState('');
  const [keywordSuccess, setKeywordSuccess] = useState('');

  // 실시간 검증 함수들
  const handleNameChange = (value: string) => {
    setSlide1NameInput(value);
    const validation = validateName(value);
    if (validation.isValid) {
      setNameError(''); // 성공 시 에러 메시지 지우기
      setNameSuccess('✅ 올바른 입력입니다!'); // 성공 메시지 추가
    } else {
      setNameError(validation.message);
      setNameSuccess(''); // 성공 메시지 지우기
    }
  };

  const handleRoleChange = (value: string) => {
    setSlide1RoleInput(value);
    const validation = validateRole(value);
    if (validation.isValid) {
      setRoleError(''); // 성공 시 에러 메시지 지우기
      setRoleSuccess('✅ 올바른 입력입니다!'); // 성공 메시지 추가
    } else {
      setRoleError(validation.message);
      setRoleSuccess(''); // 성공 메시지 지우기
    }
  };

  const handleKeywordChange = (value: string) => {
    setCurrentInput(value);
    const validation = validateKeyword(value);
    
    // 중복 검사
    const currentSlideData = slides[currentSlide];
    const isDuplicate = currentSlideData?.addedItems?.includes(value.trim());
    
    if (validation.isValid) {
      if (isDuplicate) {
        setKeywordError('이미 추가된 키워드입니다.');
        setKeywordSuccess(''); // 성공 메시지 지우기
      } else {
        setKeywordError(''); // 성공 시 에러 메시지 지우기
        setKeywordSuccess('✅ 올바른 입력입니다!'); // 성공 메시지 추가
      }
    } else {
      setKeywordError(validation.message);
      setKeywordSuccess(''); // 성공 메시지 지우기
    }
  };

  const nextSlide = () => {
    if (currentSlide === 1) {
      if (!slides[1]?.name || !slides[1]?.role) {
        alert('이름과 역할을 모두 입력해주세요.');
        return;
      }
    } else if (currentSlide > 1 && currentSlide < slides.length - 1) {
      if (!slides[currentSlide]?.addedItems || slides[currentSlide].addedItems.length === 0) {
        alert('최소 1개 이상의 키워드를 입력해주세요.');
        return;
      }
    }
    if (currentSlide < slides.length - 1) {
      setCurrentSlide(currentSlide + 1);
    }
  };

  const prevSlide = () => {
    if (currentSlide > 0) {
      setCurrentSlide(currentSlide - 1);
    }
  };

  const handleAdd = () => {
    const validation = validateKeyword(currentInput);
    if (validation.isValid) {
      // 중복 검사
      const currentSlideData = slides[currentSlide];
      if (currentSlideData?.addedItems?.includes(currentInput.trim())) {
        setKeywordError('이미 추가된 키워드입니다.');
        setKeywordSuccess(''); // 성공 메시지 초기화
        return;
      }
      
      const newSlides = [...slides];
      const slide = newSlides[currentSlide];
      if (slide && slide.addedItems) {
        slide.addedItems.push(currentInput.trim());
        setSlides(newSlides);
        setCurrentInput('');
        setKeywordError(''); // 에러 메시지 초기화
        setKeywordSuccess(''); // 성공 메시지 초기화
      }
    } else {
      setKeywordError(validation.message);
      setKeywordSuccess(''); // 성공 메시지 초기화
    }
  };

  const handleSaveName = () => {
    const validation = validateName(slide1NameInput);
    if (validation.isValid) {
      const newSlides = [...slides];
      const slide = newSlides[1];
      if (slide) {
        slide.name = slide1NameInput.trim();
        setSlides(newSlides);
        setShowSlide1NameInput(false);
        setNameError(''); // 에러 메시지 초기화
        setNameSuccess(''); // 성공 메시지 초기화
      }
    } else {
      setNameError(validation.message);
      setNameSuccess(''); // 성공 메시지 초기화
    }
  };

  const handleResetName = () => {
    setShowSlide1NameInput(true);
    setSlide1NameInput('');
    setNameError(''); // 에러 메시지 초기화
    setNameSuccess(''); // 성공 메시지 초기화
    const newSlides = [...slides];
    const slide = newSlides[1];
    if (slide) {
      slide.name = '';
      setSlides(newSlides);
    }
  };

  const handleSaveRole = () => {
    const validation = validateRole(slide1RoleInput);
    if (validation.isValid) {
      const newSlides = [...slides];
      const slide = newSlides[1];
      if (slide) {
        slide.role = slide1RoleInput.trim();
        setSlides(newSlides);
        setShowSlide1RoleInput(false);
        setRoleError(''); // 에러 메시지 초기화
        setRoleSuccess(''); // 성공 메시지 초기화
      }
    } else {
      setRoleError(validation.message);
      setRoleSuccess(''); // 성공 메시지 초기화
    }
  };

  const handleResetRole = () => {
    setShowSlide1RoleInput(true);
    setSlide1RoleInput('');
    setRoleError(''); // 에러 메시지 초기화
    setRoleSuccess(''); // 성공 메시지 초기화
    const newSlides = [...slides];
    const slide = newSlides[1];
    if (slide) {
      slide.role = '';
      setSlides(newSlides);
    }
  };

  const handleCreateFairytale = async () => {
    setIsLoading(true);
    // 옵셔널 체이닝(?.)과 null 병합 연산자(??)로 안전하게 데이터에 접근합니다.
    const fairytaleCreateRequest = {
      childName: slides[1]?.name ?? '',
      childRole: slides[1]?.role ?? '',
      characters: slides[2]?.addedItems?.join(', ') ?? '',
      place: slides[3]?.addedItems?.join(', ') ?? '',
      mood: slides[4]?.addedItems?.join(', ') ?? '',
      lesson: slides[5]?.addedItems?.join(', ') ?? '',
    };

    try {
      const response = await customFetch('https://nbe6-8-2-team07.onrender.com/fairytales', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(fairytaleCreateRequest),
      });

      if (!response.ok) {
        throw new Error(`Error: ${response.status}`);
      }

      const result = await response.json();
      console.log('Fairytale created successfully:', result);
      router.push(`/fairytale/get/${result.id}`);
    } catch (error) {
      console.error('Failed to create fairytale:', error);
      alert('동화 생성에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-screen bg-gray-100">
      {isLoading && (
        <div className="fixed inset-0 bg-gradient-to-br from-orange-100 via-yellow-50 to-amber-100 flex justify-center items-center z-50">
        <div className="text-center">
          {/* 메인 로딩 애니메이션 */}
          <div className="relative mb-8">
            {/* 책 애니메이션 */}
            <div className="relative w-32 h-24 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-r from-orange-400 to-amber-400 rounded-lg shadow-lg transform rotate-3 animate-bounce"></div>
              <div className="absolute inset-0 bg-gradient-to-r from-yellow-400 to-orange-400 rounded-lg shadow-lg animate-pulse"></div>
              <div className="absolute top-2 left-2 right-2 bottom-2 bg-white rounded opacity-90"></div>
              <div className="absolute top-4 left-4 right-4 space-y-1">
                <div className="h-1 bg-gray-300 rounded animate-pulse"></div>
                <div className="h-1 bg-gray-300 rounded animate-pulse delay-75"></div>
                <div className="h-1 bg-gray-300 rounded animate-pulse delay-150"></div>
                <div className="h-1 bg-gray-300 rounded animate-pulse delay-300"></div>
              </div>
            </div>
    
            {/* 마법 별들 */}
            <div className="absolute -top-4 -left-8 text-yellow-400 animate-bounce delay-100">✨</div>
            <div className="absolute -top-6 right-4 text-orange-400 animate-bounce delay-300">⭐</div>
            <div className="absolute top-2 -right-8 text-amber-400 animate-bounce delay-500">✨</div>
            <div className="absolute -bottom-2 -left-4 text-yellow-500 animate-bounce delay-700">🌟</div>
            <div className="absolute -bottom-4 right-2 text-orange-300 animate-bounce delay-900">✨</div>
          </div>
    
          {/* 로딩 텍스트 */}
          <div className="mb-6">
            <h2 className="text-4xl font-bold bg-gradient-to-r from-orange-500 to-amber-500 bg-clip-text text-transparent mb-3 animate-pulse">
              동화가 자라나는 중...
            </h2>
            <p className="text-lg text-gray-600 animate-fade-in-out">
              아이만의 특별한 이야기를 만들고 있어요!
            </p>
          </div>
    
          {/* 프로그레스 바 */}
          <div className="w-80 mx-auto mb-6">
            <div className="bg-orange-100 rounded-full h-3 shadow-inner">
              <div className="bg-gradient-to-r from-orange-400 to-amber-400 h-3 rounded-full shadow-sm animate-loading-progress"></div>
            </div>
          </div>
    
          {/* 로딩 단계 텍스트 (순환) */}
          <div className="text-sm text-gray-500 animate-loading-text">
            <span>상상의 나래를 펼치는 중...</span>
          </div>
        </div>
    
        {/* 추가 CSS 애니메이션을 위한 스타일 */}
        <style jsx>{`
          @keyframes loading-progress {
            0% { width: 0%; }
            25% { width: 30%; }
            50% { width: 60%; }
            75% { width: 85%; }
            100% { width: 95%; }
          }
          
          @keyframes fade-in-out {
            0%, 100% { opacity: 0.7; }
            50% { opacity: 1; }
          }
          
          @keyframes loading-text {
            0% { content: "상상의 나래를 펼치는 중..."; }
            33% { content: "마법의 이야기를 짜는 중..."; }
            66% { content: "특별한 모험을 준비하는 중..."; }
            100% { content: "아름다운 동화를 완성하는 중..."; }
          }
          
          .animate-loading-progress {
            animation: loading-progress 3s ease-in-out infinite;
          }
          
          .animate-fade-in-out {
            animation: fade-in-out 2s ease-in-out infinite;
          }
          
          .animate-loading-text {
            animation: loading-text 4s ease-in-out infinite;
          }
        `}</style>
      </div>
      )}
      <div className="relative w-full h-full p-8 bg-[#FAF9F6] rounded-lg shadow-lg flex flex-col">
        <div className="flex-grow p-4">
          {currentSlide === slides.length - 1 ? (
            // 리뷰 슬라이드
            <div className="flex flex-row space-x-4 h-full">
              <div className="flex-1 flex flex-col items-center">
                <div className="mb-4 w-150 h-150 flex items-center justify-center bg-orange-100 text-gray-500 relative">
                  {slides[currentSlide]?.image ? (
                    <img src={slides[currentSlide]?.image} alt="Slide Image" className="max-h-full max-w-full object-contain" />
                  ) : (
                    "이미지 삽입 공간"
                  )}
                </div>
              </div>
              <div className="flex-1 flex flex-col">
                <h2 className="text-3xl font-bold mb-2">{slides[currentSlide]?.title}</h2>
                <p className="text-gray-500 mb-2 text-lg">{slides[currentSlide]?.content}</p>
                <div className="mb-2">
                  {slides.slice(1, -1).map((slide, index) => (
                    <div key={index} className="p-4">
                      <h3 className="text-xl font-semibold mb-4 text-orange-500">{slide.title}</h3>
                      {/* name, role, addedItems 속성이 있는지 확인 후 렌더링 */}
                      {slide.name && (
                        <div className="mb-2">
                          <span className="text-lg">이름 : </span>
                          <div className="inline-flex items-center bg-orange-400 text-gray-50 text-xl font-medium px-2.5 py-0.5 rounded-full mr-2 mb-2">
                            <span className="whitespace-nowrap">{slide.name}</span>
                          </div>
                        </div>
                      )}
                      {slide.role && (
                        <div>
                          <span className="text-lg">역할 : </span>
                          <div className="inline-flex items-center bg-orange-400 text-gray-50 text-xl font-medium px-2.5 py-0.5 rounded-full mr-2">
                            <span className="whitespace-nowrap">{slide.role}</span>
                          </div>
                        </div>
                      )}
                      {slide.addedItems && slide.addedItems.length > 0 && (
                        <div className="mt-2 flex items-center flex-wrap">
                          {slide.addedItems.map((item, itemIndex) => (
                            <div key={itemIndex} className="inline-flex items-center bg-orange-400 text-gray-50 text-xl font-medium px-2.5 py-0.5 rounded-full mr-2 mb-2">
                              <span className="whitespace-nowrap">{item}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : currentSlide === 0 ? (
               <div className="flex justify-center items-center h-full relative">

              {/* 메인 콘텐츠 */}
              <div className="max-w-5xl mx-auto z-10 relative">
                {/* 메인 타이틀 */}
                <div className="mb-12 text-center">
                  <div className="inline-block p-6 bg-gradient-to-r from-orange-50 to-yellow-50 rounded-3xl shadow-lg mb-6">
                    <h1 className="text-5xl font-bold bg-gradient-to-r from-orange-600 to-yellow-600 bg-clip-text text-transparent mb-4 leading-tight">
                      아이와 함께 아이만의 동화를 만들어주세요!
                    </h1>
                    <div className="w-32 h-1 bg-gradient-to-r from-orange-400 to-yellow-400 rounded-full mx-auto"></div>
                  </div>
                </div>

                {/* 안내 텍스트 - 카드 형태로 업그레이드 */}
                <div className="space-y-6">
                  {/* 주인공 설정 */}
                  <div className="bg-white rounded-2xl p-6 shadow-lg border border-orange-100 transform hover:scale-105 transition-all duration-300 hover:shadow-xl">
                    <div className="flex items-start">
                      <div className="w-12 h-12 bg-gradient-to-r from-orange-300 to-amber-300 rounded-full flex items-center justify-center mr-4 mt-1 flex-shrink-0 shadow-md">
                        <span className="text-white font-bold text-xl">1</span>
                      </div>
                      <div>
                        <h3 className="text-2xl font-bold text-gray-800 mb-3 flex items-center">
                          주인공 설정 
                        </h3>
                        <p className="text-gray-600 text-lg leading-relaxed">
                          주인공의 <span className="font-semibold text-amber-600 bg-amber-50 px-2 py-1 rounded-lg">이름과 역할</span>을 하나씩 입력해요.
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* 키워드 입력 */}
                  <div className="bg-white rounded-2xl p-6 shadow-lg border border-yellow-100 transform hover:scale-105 transition-all duration-300 hover:shadow-xl">
                    <div className="flex items-start">
                      <div className="w-12 h-12 bg-gradient-to-r from-yellow-300 to-orange-300 rounded-full flex items-center justify-center mr-4 mt-1 flex-shrink-0 shadow-md">
                        <span className="text-white font-bold text-xl">2</span>
                      </div>
                      <div>
                        <h3 className="text-2xl font-bold text-gray-800 mb-3 flex items-center">
                          키워드 입력
                        </h3>
                        <p className="text-gray-600 text-lg leading-relaxed">
                          <span className="font-semibold text-amber-600 bg-amber-50 px-2 py-1 rounded-lg">등장인물 · 장소 · 분위기 · 교훈</span> 각 항목에 맞는 키워드를 자유롭게 입력할 수 있어요.
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* 동화 완성 */}
                  <div className="bg-white rounded-2xl p-6 shadow-lg border border-amber-100 transform hover:scale-105 transition-all duration-300 hover:shadow-xl">
                    <div className="flex items-start">
                      <div className="w-12 h-12 bg-gradient-to-r from-amber-300 to-yellow-300 rounded-full flex items-center justify-center mr-4 mt-1 flex-shrink-0 shadow-md">
                        <span className="text-white font-bold text-xl">3</span>
                      </div>
                      <div>
                        <h3 className="text-2xl font-bold text-gray-800 mb-3 flex items-center">
                          동화 완성
                        </h3>
                        <p className="text-gray-600 text-lg leading-relaxed">
                          마지막 슬라이드에서 입력한 내용을 확인하고, <span className="font-semibold text-amber-600 bg-amber-50 px-2 py-1 rounded-lg">동화 만들기 버튼</span>을 눌러 나만의 동화를 완성할 수 있어요!
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 하단 격려 메시지 */}
                <div className="mt-12 text-center">
                  <div className="inline-flex items-center bg-gradient-to-r from-orange-400 to-amber-400 text-white px-8 py-4 rounded-full shadow-lg animate-pulse">
                    <span className="text-xl font-semibold mr-3">함께 멋진 동화를 만들어봐요!</span>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            // 일반 슬라이드
            <div className="flex flex-row space-x-4 h-full">
              <div className="flex-1 flex flex-col items-center">
                <div className="mb-4 w-150 h-150 flex items-center justify-center bg-orange-100 text-gray-500 relative">
                  {slides[currentSlide]?.image ? (
                    <img src={slides[currentSlide]?.image} alt="Slide Image" className="max-h-full max-w-full object-contain" />
                  ) : (
                    "이미지 삽입 공간"
                  )}
                </div>
              </div>
              <div className="flex-1 flex flex-col">
                <h2 className="text-3xl font-bold mb-2">{slides[currentSlide]?.title}</h2>
                <p className="text-gray-500 mb-2 text-lg">{slides[currentSlide]?.content}</p>

                {currentSlide === 1  && (
                  <div className="mb-2">
                    {showSlide1NameInput ? (
                      <div>
                        <div className="flex mb-1">
                          <textarea
                            className="w-60 p-2 border border-gray-300 rounded-md resize-none mr-2"
                            rows={1}
                            placeholder="여기에 이름을 입력하세요."
                            value={slide1NameInput}
                            onChange={(e) => handleNameChange(e.target.value)}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter') {
                                e.preventDefault();
                                handleSaveName();
                              }
                            }}
                          ></textarea>
                          <button
                            onClick={handleSaveName}
                            className="px-2 py-2 font-bold text-white bg-orange-400 rounded hover:bg-orange-300 cursor-pointer"
                          >
                            저장
                          </button>
                        </div>
                        {nameError && (
                          <div className="text-red-500 text-sm mt-1">{nameError}</div>
                        )}
                        {nameSuccess && (
                          <div className="text-green-500 text-sm mt-1">{nameSuccess}</div>
                        )}
                      </div>
                    ) : (
                      <>
                        <span className="text-xl">이름 : </span>
                        <div className="inline-flex items-center bg-orange-400 text-gray-50 text-xl font-medium px-2.5 py-0.5 rounded-full mr-2 mb-2">
                          <p className="whitespace-nowrap">{slides[1]?.name}</p>
                          <button
                            onClick={handleResetName}
                            className="ml-1.5 -mr-0.5 w-4 h-4 inline-flex items-center justify-center rounded-full bg-orange-300 text-gray-50 hover:bg-orange-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 cursor-pointer"
                          >
                            <svg className="w-2 h-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 14 14">
                              <path stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6" />
                            </svg>
                          </button>
                        </div>
                      </>
                    )}
                    <div className="mt-4">
                      {showSlide1RoleInput ? (
                        <div>
                          <div className="flex mb-1">
                            <textarea
                              className="w-60 p-2 border border-gray-300 rounded-md resize-none mr-2"
                              rows={1}
                              placeholder="여기에 역할을 입력하세요."
                              value={slide1RoleInput}
                              onChange={(e) => handleRoleChange(e.target.value)}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter') {
                                  e.preventDefault();
                                  handleSaveRole();
                                }
                              }}
                            ></textarea>
                            <button
                              onClick={handleSaveRole}
                              className="px-2 py-2 font-bold text-white bg-orange-400 rounded hover:bg-orange-300 cursor-pointer"
                            >
                              저장
                            </button>
                          </div>
                          {roleError && (
                            <div className="text-red-500 text-sm mt-1">{roleError}</div>
                          )}
                          {roleSuccess && (
                            <div className="text-green-500 text-sm mt-1">{roleSuccess}</div>
                          )}
                        </div>
                      ) : (
                        <>
                          <span className="text-xl">역할 : </span>
                          <div className="inline-flex items-center bg-orange-400 text-gray-50 text-xl font-medium px-2.5 py-0.5 rounded-full mr-2 mb-2">
                            <p className="whitespace-nowrap">{slides[1]?.role}</p>
                            <button
                              onClick={handleResetRole}
                              className="ml-1.5 -mr-0.5 w-4 h-4 inline-flex items-center justify-center rounded-full bg-orange-300 text-gray-50 hover:bg-orange-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 cursor-pointer"
                            >
                              <svg className="w-2 h-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 14 14">
                                <path stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6" />
                              </svg>
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                )}

                {currentSlide > 1 && currentSlide < slides.length - 1 && (
                  <>
                    <div className="mb-2">
                      <div className="flex mb-1">
                        <textarea
                          className="w-60 p-2 border border-gray-300 rounded-md resize-none mr-2"
                          rows={1}
                          placeholder="여기에 키워드를 입력하세요."
                          value={currentInput}
                          onChange={(e) => handleKeywordChange(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              handleAdd();
                            }
                          }}
                        ></textarea>
                        <button
                          onClick={handleAdd}
                          className="px-2 py-2 font-bold text-white bg-orange-400 rounded hover:bg-orange-300 cursor-pointer"
                        >
                          추가
                        </button>
                      </div>
                      {keywordError && (
                        <div className="text-red-500 text-sm mt-1">{keywordError}</div>
                      )}
                      {keywordSuccess && (
                        <div className="text-green-500 text-sm mt-1">{keywordSuccess}</div>
                      )}
                    </div>
                    <div className="mb-2">
                      {slides[currentSlide]?.addedItems && slides[currentSlide].addedItems.length > 0 ? (
                        <div className="flex items-center flex-wrap">
                          {slides[currentSlide].addedItems.map((item, index) => (
                            <div key={index} className="inline-flex items-center bg-orange-400 text-gray-50 text-xl font-medium px-2.5 py-0.5 rounded-full mr-2 mb-2">
                              <span className="whitespace-nowrap">{item}</span>
                              <button
                                onClick={() => {
                                  const newSlides = [...slides];
                                  const slide = newSlides[currentSlide];
                                  if (slide && slide.addedItems) {
                                    slide.addedItems.splice(index, 1);
                                    setSlides(newSlides);
                                  }
                                }}
                                className="ml-1.5 -mr-0.5 w-4 h-4 inline-flex items-center justify-center rounded-full bg-orange-300 text-gray-50 hover:bg-orange-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 cursor-pointer"
                              >
                                <svg className="w-2 h-2" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 14 14">
                                  <path stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6" />
                                </svg>
                              </button>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-gray-500">입력된 키워드가 없습니다.</p>
                      )}
                    </div>
                  </>
                )}
              </div>
            </div>
          )}
        </div>
        <div className="sticky bottom-5 bg-[#FAF9F6] pt-4 mt-4">
          <div className="mb-2 text-2xl text-right">
            <span className="text-gray-600">
              {currentSlide + 1} / {slides.length}
            </span>
          </div>
          <div className="flex justify-end">
            <button
              onClick={prevSlide}
              disabled={currentSlide === 0}
              className="px-4 py-2 mr-2 font-bold text-white bg-orange-400 rounded disabled:bg-gray-400 hover:bg-orange-300 cursor-pointer"
            >
              이전
            </button>
            {currentSlide === 6 ? (
              <button
                onClick={handleCreateFairytale}
                className="px-4 py-2 font-bold text-white bg-orange-600 rounded hover:bg-orange-500 cursor-pointer"
              >
                동화 만들기
              </button>
            ) : (
              <button
                onClick={nextSlide}
                disabled={currentSlide === slides.length - 1}
                className="px-4 py-2 font-bold text-white bg-orange-400 rounded disabled:bg-gray-400 hover:bg-orange-300 cursor-pointer"
              >
                다음
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}