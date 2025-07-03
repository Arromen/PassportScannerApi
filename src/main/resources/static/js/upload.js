document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('fileInput');
    const fileName = document.getElementById('fileName');
    const fileCount = document.getElementById('fileCount');
    const progressBar = document.getElementById('progressBar');

    fileInput.addEventListener('change', async (e) => {
        const files = e.target.files;
        if (!files || files.length === 0) return;

        fileName.textContent = 'Подготовка файлов...';
        fileCount.textContent = `Выбрано файлов: ${files.length}`;
        progressBar.style.display = 'block';

        const formData = new FormData();
        for (let i = 0; i < files.length; i++) {
            formData.append('files', files[i]);
        }

        try {
            const response = await fetch('/api/upload-files', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const blob = await response.blob();
                const downloadUrl = window.URL.createObjectURL(blob);

                const a = document.createElement('a');
                a.href = downloadUrl;
                a.download = 'passport_images.zip';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);

                fileName.textContent = 'Все файлы успешно обработаны';
            } else {
                const errorText = await response.text();
                fileName.textContent = `Ошибка: ${errorText || 'неизвестная ошибка'}`;
            }
        } catch (error) {
            console.error('Ошибка:', error);
            fileName.textContent = 'Произошла ошибка при загрузке файлов';
        } finally {
            progressBar.style.display = 'none';
        }
    });
});

function triggerFileInput() {
    document.getElementById('fileInput').click();
}