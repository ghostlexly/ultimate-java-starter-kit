'use client';

import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {useRouter} from 'next/navigation';
import axios from 'axios';
import {wolfios} from '@/lib/wolfios';
import type {AuthResponse, MeResponse, SendCodeRequest, VerifyCodeRequest,} from '@/types/auth';

/**
 * Hook to get the current authenticated user.
 *
 * Returns `null` for anonymous users (rather than throwing) so the cache can
 * be SSR-seeded with `null` and the first client render shows the unauth state
 * without a spinner flicker.
 */
export function useAuth() {
    const user = useQuery<MeResponse | null>({
        queryKey: ['auth', 'me'],
        queryFn: async () => {
            try {
                const response = await wolfios.get<MeResponse>('/api/auth/me');

                return response.data;
            } catch (error) {
                if (axios.isAxiosError(error) && error.response?.status === 401) {
                    return null;
                }

                throw error;
            }
        },
        staleTime: Infinity,
        retry: false,
    });

    const isUnauthenticated = !user.isLoading && (user.isError || !user.data);

    // const isCustomer = !user.isLoading && user.data?.role === 'CUSTOMER';

    return {...user, isUnauthenticated};
}

/**
 * Hook to send a login code to an email address.
 */
export function useSendCode() {
    return useMutation({
        mutationFn: (data: SendCodeRequest) => wolfios.post('/api/auth/send-code', data),
    });
}

/**
 * Hook to verify a login code and authenticate the user.
 * On success, creates a customer profile if the role is CUSTOMER.
 */
export function useVerifyCode() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: VerifyCodeRequest) =>
            wolfios.post<AuthResponse>('/api/auth/verify-code', data).then((r) => r.data),
        onSuccess: async () => {
            // Invalidate auth query to refetch user data
            await queryClient.invalidateQueries({queryKey: ['auth', 'me']});
        },
    });
}

/**
 * Hook to log out the current user.
 */
export function useLogout() {
    const queryClient = useQueryClient();
    const router = useRouter();

    return useMutation({
        mutationFn: () => wolfios.post('/api/auth/logout', {}),
        onSuccess: () => {
            queryClient.clear();
            router.push('/');
        },
    });
}
