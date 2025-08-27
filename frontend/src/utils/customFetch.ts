
// 재발급 로직이 현재 진행 중인지 추적하는 플래그
let isRefreshing = false;
// 재발급이 진행되는 동안 들어온 요청들을 저장하는 배열
let failedQueue: { resolve: (token: string | null) => void; reject: (error: unknown) => void }[] = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

export const customFetch = async (url: string, options: RequestInit = {}): Promise<Response> => {
  // 모든 요청에 credentials: 'include' 옵션을 기본으로 추가
  const fetchOptions: RequestInit = {
    ...options,
    credentials: 'include',
    headers: {
      ...options.headers,
    },
  };

  // 1. 첫 번째 요청 시도
  let response = await fetch(url, fetchOptions);

  // 2. 액세스 토큰 만료(401) 시 재발급 로직 처리 (noRefresh 옵션이 없을 때만)
  // @ts-expect-error - noRefresh option is not in RequestInit type but needed for token refresh logic
  if (response.status === 401 && !options.noRefresh) {
    if (isRefreshing) {
      // 이미 재발급이 진행 중이라면, 현재 요청을 큐에 추가하고 대기
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
      .then(() => {
        // 재발급이 완료된 후, 원래 요청을 다시 시도
        return fetch(url, fetchOptions);
      })
      .catch(err => {
        return Promise.reject(err);
      });
    }

    isRefreshing = true;

    try {
      // 3. 토큰 재발급 요청
      const refreshResponse = await fetch('https://nbe6-8-2-team07.onrender.com/reissue', {
        method: 'POST',
        credentials: 'include',
      });

      if (!refreshResponse.ok) {
        // 재발급 실패 시 (리프레시 토큰 만료 등)
        // 모든 대기 중인 요청을 실패 처리하고 로그아웃 로직으로 연결
        processQueue(new Error('Session expired'), null);
        // 여기서 로그아웃 처리 또는 로그인 페이지로 리디렉션
        // window.location.href = '/login'; 
        throw new Error('Failed to refresh token');
      }

      // 4. 재발급 성공 시, 원래 요청을 다시 시도
      processQueue(null, 'new_token_placeholder'); // 대기열에 있던 요청들 재개
      response = await fetch(url, fetchOptions);

    } catch (error) {
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }

  return response;
};
