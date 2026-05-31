# Known Issues

- Xoá ảnh trên Android 11+ chưa dùng `MediaStore.createDeleteRequest()` — legacy path sẽ bị từ chối
- Duplicate detection chỉ dựa size+tên, chưa có content hash
- Permission denied không có flow recovery — app kẹt ở màn hình trắng
- Edge-to-edge bật nhưng chưa xử lý WindowInsets — nội dung bị che bởi system bars
- Room schema khai báo nhưng chưa dùng — ScanResultDao chưa được inject ở đâu
- Chưa có unit test / instrumentation test
- Chưa có Worker nào dù đã setup HiltWorkerFactory
