### Hook

```ts
import {useMutation} from '@tanstack/react-query';
import {SendSupportMessageRequest} from '@/types/support';
import {wolfios} from '@/lib/wolfios';

export function useSendSupportMessage() {
    return useMutation({
        mutationFn: (data: SendSupportMessageRequest) =>
            wolfios.post('/api/support', data).then((res) => res.data),
    });
}
```

### Interface

```ts
export interface SendSupportMessageRequest {
    email: string;
    message: string;
}
```

### Usage

```tsx
const form = useForm<SupportFormValues>({
    defaultValues: {email: '', message: ''},
});
const sendSupportMessage = useSendSupportMessage();

async function handleSubmit(values: SupportFormValues) {
    // The mutation will still throw an error if it fails, even with the onError
    await sendSupportMessage.mutateAsync(values, {
        onSuccess: async () => {
            toast.success(
                'Nous avons bien reçu votre message. Nous vous répondrons dans les plus brefs délais.',
            );
            form.reset();
        },
        onError: (error) => {
            // Field-level errors are shown inline by EmailStep — only toast for generic errors
            if (hasFieldErrors(error)) {
                return;
            }


            toast.error(getErrorMessage(error, "Erreur lors de l'envoi du code"));
        },
    });
}

useBackendFormErrors(form, sendSupportMessage.error);
```