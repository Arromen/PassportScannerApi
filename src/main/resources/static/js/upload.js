document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('fileInput');
    const fileName = document.getElementById('fileName');
    const statusMessage = document.getElementById('statusMessage');
    const progressBar = document.getElementById('progressBar');

    fileInput.addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        fileName.textContent = `Выбран файл: ${file.name}`;
        statusMessage.textContent = 'Обработка...';
        progressBar.style.display = 'block';

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('/api/upload-file', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const blob = await response.blob();
                const downloadUrl = window.URL.createObjectURL(blob);

                const contentDisposition = response.headers.get('Content-Disposition');
                let downloadName = 'passport_face.png';

                if (contentDisposition) {
                    const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
                    if (filenameMatch && filenameMatch[1]) {
                        downloadName = filenameMatch[1];
                    }
                }

                const a = document.createElement('a');
                a.href = downloadUrl;
                a.download = downloadName;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);

                statusMessage.textContent = 'Файл успешно обработан и скачан';
                statusMessage.style.color = 'green';
            } else {
                const errorText = await response.text();
                statusMessage.textContent = `Ошибка: ${errorText || 'неизвестная ошибка'}`;
                statusMessage.style.color = 'red';
            }
        } catch (error) {
            console.error('Ошибка:', error);
            statusMessage.textContent = 'Произошла ошибка при загрузке файла';
            statusMessage.style.color = 'red';
        } finally {
            progressBar.style.display = 'none';
        }
    });
});

function triggerFileInput() {
    document.getElementById('fileInput').click();
}