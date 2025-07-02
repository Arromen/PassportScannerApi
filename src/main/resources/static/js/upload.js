document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('fileInput');
    const fileName = document.getElementById('fileName');
    const progressBar = document.getElementById('progressBar');

    fileInput.addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (file) {
            fileName.textContent = `Выбран файл: ${file.name}`;
            progressBar.style.display = 'block';

            const formData = new FormData();
            formData.append('file', file);

            try {
                const response = await fetch('/api/upload-files', {
                    method: 'POST',
                    body: formData
                });

                if (response.ok) {
                    const blob = await response.blob();
                    const downloadUrl = window.URL.createObjectURL(blob);
                    const downloadFileName = response.headers.get('Content-Disposition')
                        ?.split('filename=')[1]
                        ?.replace(/"/g, '') || 'processed_passport.jpg';

                    // Создаем ссылку для автоматического скачивания
                    const a = document.createElement('a');
                    a.href = downloadUrl;
                    a.download = downloadFileName;
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);

                    fileName.textContent = 'Файл успешно обработан и скачан';
                } else {
                    fileName.textContent = 'Ошибка при обработке файла';
                }
            } catch (error) {
                console.error('Ошибка:', error);
                fileName.textContent = 'Произошла ошибка при загрузке файла';
            } finally {
                progressBar.style.display = 'none';
            }
        }
    });
});

function triggerFileInput() {
    document.getElementById('fileInput').click();
}