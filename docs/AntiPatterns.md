# Anti-Patterns

> **Tại sao tài liệu này tồn tại:** "Cleaner app" là một trong những thể loại có nhiều scam nhất trên Play Store. CoreClean được thiết kế **ngược lại** với những app đó. Tài liệu này liệt kê những gì KHÔNG BAO GIỜ được làm — nếu một feature/code vi phạm mục nào dưới đây, từ chối merge.

## 1. Phantom numbers — số liệu giả

❌ **Không được:**
- Hiển thị "Đã giải phóng 2.3 GB!" khi thực tế chỉ xoá vài MB cache.
- Animated counter chạy quá tốc độ thực tế của thao tác.
- "Found 1,247 junk files" khi danh sách thật chỉ 12 file.
- Multiply size by random factor để con số trông "ấn tượng".

✅ **Đúng:**
- `toReadableSize(actualBytes)` — đúng byte, đúng đơn vị (KB/MB/GB chuẩn IEC).
- Counter chạy theo tiến độ thật của coroutine.
- Empty state thành thật: "Không tìm thấy ảnh trùng lặp" thay vì giả vờ đang scan.

## 2. Scareware — gây sợ hãi giả

❌ **Không được:**
- "⚠️ Pin xuống cấp nghiêm trọng!" khi pin chỉ là 85% health.
- "🚨 Điện thoại đang chậm 73%!" — không có thước đo nào cho con số này.
- Red exclamation icons cho trạng thái bình thường.
- Notification "Phát hiện 500 file rác cần dọn ngay" lặp đi lặp lại.

✅ **Đúng:**
- Trạng thái pin hiển thị raw value từ BatteryManager kèm giải thích: "Health: GOOD".
- Notification chỉ gửi khi user opt-in trong Settings, và **mỗi tuần tối đa 1 lần**.
- Tone trung tính: "Bạn có 1.2 GB ảnh trùng" thay vì "DANH BẠ ĐANG CÓ NGUY CƠ!".

## 3. Fake cleaning actions

❌ **Không được:**
- Nút "Tăng tốc RAM" gọi `System.gc()` rồi thông báo "Tăng 40% tốc độ". Android 8+ không cho kill process khác.
- "Optimize battery" không làm gì ngoài hiển thị progress bar 3 giây.
- "Boost CPU" — Android không có API để app nâng CPU governor.

✅ **Đúng:**
- Nếu không xoá được (vd: APP_CACHE trên Android 8+), **nói rõ**: "Mở App Manager trong Cài đặt để xoá" + nút mở Intent.
- Force-stop là quyền hệ thống → không show button đó.
- RAM screen chỉ **hiển thị**, không có "Boost" button.

## 4. Dark patterns trong consent

❌ **Không được:**
- Bật mặc định crash reporting / analytics → user phải tự tắt.
- "Cấp quyền truy cập tất cả file" với mô tả mơ hồ.
- Onboarding gài permission "MANAGE_EXTERNAL_STORAGE" mà không justify rõ.
- Confirm dialog "Bạn có chắc?" → nút No ẩn dưới, nút Yes nổi bật.

✅ **Đúng:**
- Mọi telemetry **opt-in** mặc định OFF.
- Mỗi permission có 1 câu giải thích cụ thể trong Onboarding.
- Confirm dialog: 2 button equal weight (Cancel / Delete), Delete màu đỏ.

## 5. Permission creep

❌ **Không được:**
- Xin `READ_PHONE_STATE`, `ACCESS_FINE_LOCATION`, `CAMERA` cho 1 cleaner app — không liên quan.
- Xin `MANAGE_EXTERNAL_STORAGE` chỉ để đếm file (dùng MediaStore là đủ).
- Xin tất cả quyền ngay onboarding step 1 — phải chia theo feature thực sự cần.

✅ **Đúng:**
- Chỉ xin permission khi module cần (lazy permission), tracked trong [`Permissions.md`](Permissions.md).
- Mỗi permission có Fallback rõ ràng — denied vẫn dùng được app (chỉ mất feature đó).

## 6. UI lying about state

❌ **Không được:**
- Spinner xoay 5 giây giả vờ "deep scan" khi thực ra chỉ đọc 1 SharedPreferences.
- Progress bar không reflect tiến độ thật → user lừa cảm giác "đang làm việc nặng".

✅ **Đúng:**
- Spinner = thực sự đang chờ I/O / network / hash compute.
- Progress = `processed / total` thật, không giả tốc.

## 7. Notification spam

❌ **Không được:**
- Gửi thông báo hàng ngày "Hôm nay bạn có 500 MB junk cần dọn!" mà không user request.
- Notification dẫn về ad / cross-promo app khác.

✅ **Đúng:**
- Mặc định KHÔNG có recurring notification.
- Worker chỉ log local, không notify trừ khi user bật trong Settings.
- Mỗi loại notification có Notification Channel riêng → user dễ tắt.

## 8. Ads & monetization

CoreClean (hiện tại) **không có ads, không có IAP**. Nếu sau này có:
- Không bao giờ chèn ads trong luồng "Cleaning result" (gây hiểu nhầm).
- Không full-screen interstitial sau khi user nhấn nút "Clean".
- Banner ads (nếu có) phải được label "Advertisement" rõ ràng.

## 9. Review prompts

❌ Không hiện "⭐ Rate us" pop-up sau action quan trọng (user đang cần xác nhận, không phải đánh giá).
✅ Chỉ nhắc rating qua Play In-App Review API, tối đa 1 lần / 30 ngày, sau session thành công.

## 10. Auto-start on boot

❌ Không tự bật BroadcastReceiver `BOOT_COMPLETED` để chạy worker ngay. Chỉ enqueue Worker — Android sẽ schedule khi điều kiện thoả.
✅ `BOOT_COMPLETED` chỉ để **re-schedule** Worker (WorkManager tự handle).

---

## 11. Battery prediction phải là ước tính, không là khẳng định

❌ **Không được:**
- "Còn đúng 3 giờ 24 phút" — chính xác tới phút dễ gây trust quá mức.
- Không hiển thị disclaimer.
- Cập nhật liên tục tạo cảm giác "thời gian thật".

✅ **Đúng:**
- Luôn có dòng "(Ước tính dựa trên thói quen 24 giờ)" ngay dưới con số.
- Khi chưa đủ dữ liệu (< 4 điểm): "Đang thu thập dữ liệu..." thay vì tính ra số giả.
- Code verify: `BatteryPredictionCard` buộc phải render `battery_prediction_disclaimer` khi có estimate.

---

**Reviewer checklist khi PR có UI numbers hoặc permission:**
- [ ] Số hiển thị có nguồn rõ ràng từ API hệ thống?
- [ ] Có spinner/progress nào giả tốc không?
- [ ] Tone của copy có trung tính không, hay đang "scary"?
- [ ] Permission mới có justify trong Permissions.md không?
- [ ] Có default opt-in nào nguy hiểm không?
