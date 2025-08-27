'use client';

export default function IntroductionPage() {
  return (
    <main className="py-12 space-y-14 text-gray-800">
      <section className="w-full py-12 md:py-16 lg:py-16 bg-[#FFE0B5] relative">
        <div className="absolute bottom-0 left-0 right-0 max-w-5xl mx-auto px-6 pb-6">
          <h1 className="text-5xl font-extrabold text-orange-500 tracking-tight">
            동화공방 소개
          </h1>
        </div>
      </section>

      <section className="space-y-4">
        <div className="max-w-5xl mx-auto px-6">
          <h1 className="text-3xl font-bold text-orange-400 mb-2">아이의 상상력을 현실로, 우리 가족의 특별한 이야기 만들기</h1>
          <p className="text-lg text-gray-600">누구나 작가가 될 수 있는 AI 동화 이야기 플랫폼</p>
          <p className="text-lg text-gray-700">
            <strong>우리 아이의 생각과 꿈이 동화가 됩니다.</strong><br />
            동화공방은 아이가 선택한 키워드를 바탕으로, 아이만을 위한 특별한 이야기를 만듭니다.<br />
            아이는 부모님과 함께 이야기를 읽고, 생각을 나누며 따뜻한 시간을 보낼 수 있습니다.
          </p>
        </div>
      </section>

      <section className="space-y-4">
        <div className="max-w-5xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-orange-400 mb-2">비전과 미션</h2>
          <p className="text-lg text-gray-700">
            저희는 <strong>아이의 상상력과 창의력</strong>이 가장 강력한 배움의 도구라고 믿습니다.<br />
            <strong>가족과 함께하는 시간</strong>이 <strong>아이의 정서 발달</strong>에 깊은 영향을 준다고 생각합니다.
          </p>
          <p className="text-lg text-gray-700">
            그래서 동화공방은 단순한 동화 생성이 아닌,<br />
            <strong>아이의 눈높이에 맞춘 이야기</strong>와 <strong>가족간의 소통과 추억</strong>을 함께 담아냅니다.
          </p>
        </div>
      </section>

      <section className="space-y-4">
        <div className="max-w-5xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-orange-400 mb-2">기능 소개</h2>
          <ul className="list-disc list-inside text-lg text-gray-700 space-y-1 mb-2">
            <li>키워드 기반 AI 동화 자동 생성</li>
            <li>아이 이름, 성격, 관심사를 반영한 캐릭터 생성</li>
            <li>장소, 분위기, 교훈을 직접 설정 가능</li>
            <li>생성된 동화 저장 및 다시보기 기능</li>
          </ul>
          <p className="text-lg text-gray-700">
            가장 중요한 건 기술이 아니라 <strong>아이의 추억과 경험</strong>입니다.<br />
          </p>
        </div>
      </section>

      <section>
        <div className="max-w-5xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-orange-400 mb-3">지금, 우리 아이만의 이야기를 시작해보세요</h2>
          <p className="text-lg text-gray-700">          
            세상에 단 하나뿐인 특별한 동화를 함께 만들어보세요
          </p>
        </div>
      </section>
    </main>
  );
}