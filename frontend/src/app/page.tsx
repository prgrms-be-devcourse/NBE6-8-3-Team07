'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useState } from 'react';

export default function Home() {
  const fairytaleExamples = [
    {
      title: "용감한 아기 사자 레오",
      description: "작은 아기 사자 레오가 숲 속 친구들과 함께 용기를 찾아 떠나는 이야기",
      image: "/images/image5.png",
    },
    {
      title: "반짝이는 별똥별의 비밀",
      description: "하늘에서 떨어진 별똥별이 작은 소녀에게 들려주는 신비로운 모험 이야기",
      image: "/images/image6.png",
    },
    {
      title: "꼬마 마법사 릴리의 첫 주문",
      description: "엉뚱한 꼬마 마법사 릴리가 실수로 걸어버린 첫 주문으로 벌어지는 소동",
      image: "/images/image7.png",
    },
  ];

  const [currentCardIndex, setCurrentCardIndex] = useState(0);

  const handlePrev = () => {
    setCurrentCardIndex((prevIndex) =>
      prevIndex === 0 ? fairytaleExamples.length - 1 : prevIndex - 1
    );
  };

  const handleNext = () => {
    setCurrentCardIndex((prevIndex) =>
      prevIndex === fairytaleExamples.length - 1 ? 0 : prevIndex + 1
    );
  };

  return (
    <main className="flex flex-col items-center justify-center">
      {/* Hero Section */}
      <section className="w-full py-12 md:py-24 lg:py-32">
        <div className="container mx-auto px-4 md:px-6">
          <div className="grid gap-6 lg:grid-cols-2 lg:gap-12 xl:grid-cols-2">
            <div className="flex flex-col justify-center space-y-4">
              <div className="space-y-2">
                <h1 className="text-3xl font-bold tracking-tighter sm:text-5xl xl:text-6xl/none">
                  키워드 몇 개로, <br />
                  아이의 상상이 동화가 됩니다.
                </h1>
                <p className="max-w-[600px] text-gray-500 md:text-xl">
                  아이와 함께 키워드를 정하고, <br />
                  AI가 만들어주는 동화를 함께 읽어보세요. <br />
                  소중한 추억이 담긴 이야기가 아이의 상상력을 키워줍니다.
                </p>
              </div>
              <div className="w-full max-w-sm space-y-2">
                <Link
                  href="/fairytale/post"
                  className="inline-flex h-10 items-center justify-center rounded-md bg-orange-400 px-4 py-6 text-2xl font-medium text-gray-50 shadow transition-colors hover:bg-orange-300 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-gray-950 disabled:pointer-events-none disabled:opacity-50"
                >
                  아이와 함께 동화쓰기
                </Link>
              </div>
            </div>
            <div>
              {/* 이미지가 들어갈 공간 */}
              <Image src="/images/image1.png" alt="메인 이미지" width={500} height={500} style={{objectFit: 'cover'}}/>
            </div>
          </div>
        </div>
      </section>

      {/* How it works Section */}
      <section className="w-full py-12 md:py-24 lg:py-32 bg-[#FFE0B5]">
        <div className="container mx-auto px-4 md:px-6">
          <div className="flex flex-col items-center justify-center space-y-4 text-center mb-12">
            <div className="space-y-2">
              <h2 className="text-3xl font-bold tracking-tighter sm:text-5xl">어떻게 작동하나요?</h2>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-center">
            <div>
              <div className="bg-gray-50 h-72 w-72 rounded-lg mx-auto mb-4 overflow-hidden">
                {/* 이미지가 들어갈 공간 */}
                <Image src="/images/image2.png" alt="메인 이미지" width={400} height={400} style={{objectFit: 'cover'}}/>
              </div>
              <p className="text-2xl font-bold">
                키워드를 입력해요
              </p>
              <p className="text-lg text-gray-600">
                아이와 함께 떠오르는 단어를 적어보세요 <br />
                좋아하는 동물, 장소, 분위기 등을 자유롭게 입력하세요
              </p>
            </div>
            <div>
              <div className="bg-gray-50 h-72 w-72 rounded-lg mx-auto mb-4 overflow-hidden">
                {/* 이미지가 들어갈 공간 */}
                <Image src="/images/image3.png" alt="메인 이미지" width={400} height={400} style={{objectFit: 'cover'}}/>
              </div>
              <p className="text-2xl font-bold">
                AI가 동화를 만들어줘요
              </p>
              <p className="text-lg text-gray-600">
                입력한 키워드로 이야기를 지어드려요 <br />
                아이의 상상력을 가득 채워줄 동화가 순식간에 완성돼요
              </p>
            </div>
            <div>
              <div className="bg-gray-50 h-72 w-72 rounded-lg mx-auto mb-4 overflow-hidden">
                {/* 이미지가 들어갈 공간 */}
                <Image src="/images/image4.png" alt="메인 이미지" width={400} height={400} style={{objectFit: 'cover'}}/>
              </div>
              <p className="text-2xl font-bold">
                함께 읽고 저장해요
              </p>
              <p className="text-lg text-gray-600">
                세상에 하나뿐인 동화를 아이와 함께 즐기세요 <br />
                마음에 들면 언제든 다시 읽을 수 있어요
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Showcase Section */}
      <section className="w-full py-12 md:py-24 lg:py-32">
        <div className="container mx-auto px-4 md:px-6">
          <div className="flex flex-col items-center justify-center space-y-4 text-center">
            <div className="space-y-2">
              <h2 className="text-3xl font-bold tracking-tighter sm:text-5xl">이런 동화가 나와요!</h2>
            </div>
            <div className="relative w-full max-w-2xl mx-auto">
              <div className="overflow-hidden relative h-96">
                {fairytaleExamples.map((fairytale, index) => (
                  <div
                    key={index}
                    className={`absolute w-full h-full transition-all duration-500 ease-in-out transform
                      ${index === currentCardIndex ? 'translate-x-0 opacity-100' : 'translate-x-full opacity-0'}
                      ${index < currentCardIndex ? '-translate-x-full' : ''}
                    `}
                  >
                    <div className="bg-white rounded-lg shadow-lg p-6 flex flex-col items-center justify-center h-full">
                      <div className="bg-[#FFD6A5] h-48 w-48 rounded-lg mb-4 flex items-center justify-center">
                        {/* 이미지가 들어갈 공간 */}
                        <Image
                          src={fairytale.image}
                          alt={fairytale.title}
                          width={192}
                          height={192}
                          className="rounded-lg object-cover"
                        />
                      </div>
                      <h3 className="text-2xl font-bold mb-2">{fairytale.title}</h3>
                      <p className="text-gray-600 text-center">
                        {fairytale.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
              <button
                onClick={handlePrev}
                className="absolute -left-24 top-1/2 -translate-y-1/2 text-gray-800 text-7xl z-10 cursor-pointer"
              >
                &#8249;
              </button>
              <button
                onClick={handleNext}
                className="absolute -right-24 top-1/2 -translate-y-1/2 text-gray-800 text-7xl z-10 cursor-pointer"
              >
                &#8250;
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="w-full py-12 md:py-24 lg:py-32 bg-[#FFF8D6]">
        <div className="container mx-auto px-4 md:px-6">
          <div className="flex flex-col items-center justify-center space-y-4 text-center">
            <div className="space-y-2">
              <h2 className="text-3xl font-bold tracking-tighter sm:text-5xl mb-7">무엇을 기반으로 하나요?</h2>
              <p className="max-w-[900px] text-gray-600 md:text-xl/relaxed lg:text-base/relaxed xl:text-xl/relaxed">
                Gemini 기반 생성
              </p>
              <p className="max-w-[900px] text-gray-600 md:text-xl/relaxed lg:text-base/relaxed xl:text-xl/relaxed">
                연령 적합한 문장 구성
              </p>
              <p className="max-w-[900px] text-gray-600 md:text-xl/relaxed lg:text-base/relaxed xl:text-xl/relaxed">
                감정 / 교훈 중심 이야기 구조
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="w-full py-12 md:py-24 lg:py-32">
        <div className="container mx-auto grid items-center justify-center gap-4 px-4 text-center md:px-6">
          <div className="space-y-3">
            <h2 className="text-3xl font-bold tracking-tighter md:text-4xl/tight mb-10">
              지금, 우리 아이의 동화를 시작해보세요!
            </h2>
          </div>
          <div className="mx-auto w-full max-w-sm space-y-2">
            <Link
              href="/fairytale/post"
                            className="inline-flex h-10 items-center justify-center rounded-md bg-orange-400 px-4 py-6 text-2xl font-medium text-gray-50 shadow transition-colors hover:bg-orange-300 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-gray-950 disabled:pointer-events-none disabled:opacity-50"
            >
              아이와 함께 동화쓰기
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}