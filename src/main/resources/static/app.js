const conversation = document.querySelector("#conversation");
const welcome = document.querySelector("#welcome");
const messages = document.querySelector("#messages");
const input = document.querySelector("#message");
const modeSelect = document.querySelector("#mode");
const sendButton = document.querySelector("#send");
const pdfUpload = document.querySelector("#pdf-upload");
const attachPdfButton = document.querySelector("#attach-pdf");
const documentList = document.querySelector("#document-list");
const uploadStatus = document.querySelector("#upload-status");

function addMessage(role, text, isError = false, sources = []) {
    const article = document.createElement("article");
    article.className = `message ${role}${isError ? " error" : ""}`;

    const avatar = document.createElement("div");
    avatar.className = `avatar${role === "user" ? " user" : ""}`;
    avatar.textContent = role === "user" ? "Bạn" : "S";

    const body = document.createElement("div");
    body.className = "message-body";
    const label = document.createElement("div");
    label.className = "message-label";
    label.textContent = role === "user" ? "Bạn" : "Spring AI";
    const content = document.createElement("div");
    content.className = "message-content";
    content.textContent = text;
    body.append(label, content);
    if (sources.length > 0) {
        const sourceList = document.createElement("small");
        sourceList.className = "message-sources";
        sourceList.textContent = `Nguồn: ${sources.join(", ")}`;
        body.append(sourceList);
    }
    article.append(avatar, body);
    messages.append(article);
    conversation.scrollTop = conversation.scrollHeight;
    return article;
}

function addTyping() {
    const article = addMessage("assistant", "");
    const content = article.querySelector(".message-content");
    content.setAttribute("aria-label", "AI đang trả lời");
    content.innerHTML = '<span class="typing"><i></i><i></i><i></i></span>';
    return article;
}

function showUploadStatus(message, isError = false) {
    uploadStatus.textContent = message;
    uploadStatus.classList.toggle("error", isError);
}

async function loadDocuments() {
    try {
        const response = await fetch("/api/documents");
        const documents = await response.json();
        if (!response.ok) throw new Error(documents.error || "Không tải được danh sách PDF.");

        documentList.replaceChildren();
        for (const indexedDocument of documents) {
            const chip = document.createElement("span");
            chip.className = "document-chip";
            const name = document.createElement("span");
            name.className = "document-chip-name";
            name.title = indexedDocument.fileName;
            name.textContent = indexedDocument.fileName;
            const details = document.createElement("small");
            details.textContent = `${indexedDocument.chunks} đoạn`;
            const remove = document.createElement("button");
            remove.className = "remove-document";
            remove.type = "button";
            remove.setAttribute("aria-label", `Xóa ${indexedDocument.fileName}`);
            remove.textContent = "×";
            remove.addEventListener("click", () => removeDocument(indexedDocument.id, indexedDocument.fileName));
            chip.append(name, details, remove);
            documentList.append(chip);
        }
    } catch (error) {
        showUploadStatus(error.message || "Không tải được danh sách PDF.", true);
    }
}

async function uploadPdf(file) {
    if (!file) return;
    attachPdfButton.disabled = true;
    showUploadStatus(`Đang đọc và lập chỉ mục ${file.name}...`);
    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch("/api/documents", { method: "POST", body: formData });
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Không thể lập chỉ mục PDF.");
        await loadDocuments();
        showUploadStatus(`Đã nạp ${result.fileName}: ${result.pages} trang, ${result.chunks} đoạn.`);
    } catch (error) {
        showUploadStatus(error.message || "Không thể đọc PDF.", true);
    } finally {
        attachPdfButton.disabled = false;
        pdfUpload.value = "";
    }
}

async function removeDocument(id, fileName) {
    try {
        const response = await fetch(`/api/documents/${encodeURIComponent(id)}`, { method: "DELETE" });
        if (!response.ok) {
            const result = await response.json();
            throw new Error(result.error || `Không thể xóa ${fileName}.`);
        }
        await loadDocuments();
        showUploadStatus(`Đã xóa ${fileName} khỏi bộ nhớ tìm kiếm.`);
    } catch (error) {
        showUploadStatus(error.message || "Không thể xóa PDF.", true);
    }
}

async function sendMessage(text = input.value, mode = modeSelect.value) {
    const message = text.trim();
    if (!message || sendButton.disabled) return;

    welcome.hidden = true;
    addMessage("user", message);
    input.value = "";
    input.style.height = "auto";
    sendButton.disabled = true;
    const typing = addTyping();

    try {
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message, mode })
        });
        const data = await response.json();
        typing.remove();
        if (!response.ok) throw new Error(data.error || "Đã xảy ra lỗi. Vui lòng thử lại.");
        addMessage("assistant", data.reply, false, data.sources || []);
    } catch (error) {
        typing.remove();
        addMessage("assistant", error.message || "Không kết nối được máy chủ.", true);
    } finally {
        sendButton.disabled = false;
        input.focus();
    }
}

sendButton.addEventListener("click", () => sendMessage());
attachPdfButton.addEventListener("click", () => pdfUpload.click());
pdfUpload.addEventListener("change", () => uploadPdf(pdfUpload.files[0]));
input.addEventListener("input", () => {
    input.style.height = "auto";
    input.style.height = `${Math.min(input.scrollHeight, 160)}px`;
});
input.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
});

document.querySelector("#new-chat").addEventListener("click", () => {
    messages.replaceChildren();
    welcome.hidden = false;
    input.value = "";
    modeSelect.value = "chat";
    showUploadStatus("");
    input.focus();
});

loadDocuments();
