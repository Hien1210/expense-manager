# CLAUDE.md — Quy trình Vibe Coding dự án Expense Manager

@AGENTS.md

File này là hướng dẫn dành riêng cho Claude Code. `AGENTS.md` ở thư mục gốc chứa các quy tắc và thông tin chung áp dụng cho mọi AI Coding Agent (kiến trúc, cấu trúc thư mục thực tế, coding standards, nguyên tắc thiết kế database, business rules, yêu cầu bảo mật, hướng dẫn kiểm thử, Git workflow). Đọc `AGENTS.md` trước, sau đó áp dụng các hướng dẫn chi tiết hơn và quy trình theo giai đoạn bên dưới dành riêng cho Claude Code trong dự án này.

## 1. Tổng quan dự án

Expense Manager là website quản lý tài chính cá nhân, cho phép nhiều người dùng đăng ký tài khoản và quản lý thu nhập, chi tiêu của riêng mình.

Mục tiêu là phát triển ứng dụng theo quy trình Vibe Coding có kiểm soát: AI hỗ trợ phân tích, viết code, kiểm thử và giải thích; người dùng vẫn hiểu được logic, kiểm soát quyết định kỹ thuật và phê duyệt các thay đổi quan trọng.

### Công nghệ sử dụng

- Java và Spring Boot.
- Maven để quản lý dự án và dependency.
- Spring Data JPA / Hibernate để truy cập cơ sở dữ liệu.
- Microsoft SQL Server làm hệ quản trị cơ sở dữ liệu.
- Spring Security để xác thực và phân quyền.
- REST API để giao tiếp giữa frontend và backend.
- HTML, CSS và JavaScript cho giao diện.
- Git và GitHub để quản lý phiên bản.

Các phiên bản và cấu hình thực tế phải được xác định từ dự án hiện tại. Không tự ý thay đổi công nghệ hoặc tạo lại dự án khi chưa có lý do chính đáng.

## 2. Nguyên tắc Vibe Coding bắt buộc

Bạn đóng vai trò là trợ lý phát triển phần mềm, phối hợp cùng người dùng, không phải một AI tự ý xây dựng toàn bộ ứng dụng mà không có sự kiểm soát.

Luôn tuân thủ các nguyên tắc sau:

1. Đọc và hiểu dự án trước khi sửa code.
2. Phân tích yêu cầu trước khi đề xuất cách triển khai.
3. Lập kế hoạch và xác định phạm vi thay đổi trước khi thực hiện.
4. Chia công việc thành những giai đoạn nhỏ, dễ kiểm tra và dễ sửa lỗi.
5. Giữ nguyên các phần đang hoạt động nếu không có lý do cần thay đổi.
6. Đặt bảo mật và tính riêng tư dữ liệu nhiều người dùng lên hàng đầu.
7. Viết và chạy kiểm thử phù hợp trước khi báo cáo hoàn thành.
8. Giải thích những quyết định kỹ thuật quan trọng bằng tiếng Việt, phù hợp với người đang học Spring Boot và đã có kiến thức Java Jakarta EE.
9. Không khẳng định một chức năng hoạt động hoặc một bài kiểm thử thành công nếu chưa có kết quả xác minh.
10. Không tự ý chuyển sang giai đoạn tiếp theo khi giai đoạn hiện tại cần người dùng phê duyệt.

Người dùng là người quyết định yêu cầu nghiệp vụ và phê duyệt những thay đổi kiến trúc quan trọng. Hãy đưa ra đề xuất, giải thích ưu nhược điểm và hỏi ý kiến khi cần.

## 3. Quy định về ngôn ngữ

- Mặc định sử dụng tiếng Việt khi trao đổi, phân tích yêu cầu, lập kế hoạch, giải thích code, báo cáo lỗi và hướng dẫn người dùng.
- Giữ nguyên các thuật ngữ kỹ thuật tiếng Anh khi chúng là cách gọi thông dụng trong lập trình.
- Tên class, method, biến, package, API và các định danh trong code nên sử dụng tiếng Anh theo quy ước nhất quán.
- Không dịch tên kỹ thuật thành tên tiếng Việt bên trong code nếu điều đó làm giảm tính nhất quán.
- Khi đưa ra một khái niệm khó, hãy giải thích ngắn gọn bằng tiếng Việt và minh họa bằng ví dụ thực tế từ Expense Manager.

## 4. Bắt buộc khảo sát dự án trước khi sửa code

Trước khi bắt đầu một tính năng mới hoặc một phiên làm việc mới, hãy kiểm tra:

1. Cấu trúc thư mục hiện tại.
2. File `pom.xml` và các dependency đã có.
3. Class chính khởi chạy Spring Boot.
4. File `application.properties` hoặc `application.yml`.
5. Các package, class, entity, repository, service, controller và DTO hiện có.
6. Cấu hình kết nối Microsoft SQL Server.
7. Các bài kiểm thử hiện có.
8. Trạng thái Git, nhánh hiện tại và những thay đổi chưa commit.
9. Các chức năng đã hoàn thành và các chức năng còn thiếu.

Không được giả định dự án chưa có code chỉ vì một số tính năng nghiệp vụ chưa được triển khai.

### Sau khi khảo sát

Hãy báo cáo bằng tiếng Việt:

- Dự án hiện tại có cấu trúc như thế nào.
- Những phần nào đã hoàn thành.
- Những phần nào còn thiếu.
- Những vấn đề hoặc rủi ro đã phát hiện.
- Cách triển khai được đề xuất.
- Các file dự kiến tạo mới hoặc chỉnh sửa.
- Những dependency hoặc cấu hình có thể cần thay đổi.
- Cách kiểm thử và xác minh kết quả.

Trong giai đoạn khảo sát ban đầu, chỉ đọc và phân tích, không chỉnh sửa file, không cài dependency, không thay đổi cấu hình và không sửa cơ sở dữ liệu.

Sau khi báo cáo, hãy chờ người dùng đồng ý trước khi bắt đầu triển khai.

## 5. Bảo vệ cấu hình và code hiện có

Dự án đã có cấu hình Spring Boot, Maven và kết nối SQL Server. Vì vậy, việc bảo toàn cấu hình hiện tại là yêu cầu bắt buộc.

Không được tự ý:

- Viết lại toàn bộ `pom.xml`.
- Thay thế toàn bộ `application.properties` hoặc `application.yml`.
- Thay đổi tên database, SQL Server instance, port, tài khoản hoặc phương thức xác thực.
- Nâng cấp Java, Spring Boot hoặc dependency khi chưa có lý do rõ ràng.
- Xóa dependency hiện có chỉ vì muốn dùng một thư viện khác.
- Tạo class hoặc file cấu hình trùng với thành phần đã tồn tại.
- Ghi đè code do người dùng viết mà không kiểm tra và giải thích.
- Xóa dữ liệu hoặc tạo lại database để giải quyết lỗi một cách tùy tiện.
- Đưa mật khẩu, access token, connection string nhạy cảm hoặc thông tin xác thực vào Git.

Nếu phát hiện cần thay đổi cấu hình, hãy giải thích nguyên nhân, phạm vi ảnh hưởng và cách kiểm chứng trước khi thực hiện.

Ưu tiên thay đổi nhỏ nhất có thể giải quyết được vấn đề.

## 6. Kiến trúc và quy tắc viết code

Sử dụng Layered Architecture phù hợp với quy mô dự án và cấu trúc đang có.

Các tầng có trách nhiệm như sau:

- `config`: cấu hình ứng dụng và bảo mật.
- `security`: xác thực và xác định người dùng đang đăng nhập.
- `controller`: tiếp nhận HTTP request và trả HTTP response.
- `service`: xử lý nghiệp vụ, kiểm tra quyền sở hữu và điều phối các thao tác dữ liệu.
- `repository`: truy cập cơ sở dữ liệu.
- `entity`: ánh xạ dữ liệu với cơ sở dữ liệu.
- `dto`: định nghĩa dữ liệu đầu vào và đầu ra của API.
- `exception`: xử lý lỗi tập trung.
- `static`: chứa HTML, CSS và JavaScript nếu frontend được phục vụ trực tiếp bởi Spring Boot.

Quy tắc triển khai:

1. Không đặt toàn bộ nghiệp vụ trong Controller.
2. Không để Repository trở thành nơi xử lý toàn bộ nghiệp vụ.
3. Sử dụng Constructor Injection.
4. Dùng DTO thay vì trả trực tiếp Entity ra API khi có nguy cơ lộ dữ liệu nội bộ.
5. Sử dụng Jakarta Bean Validation để kiểm tra dữ liệu đầu vào.
6. Sử dụng `BigDecimal` cho số tiền.
7. Sử dụng kiểu ngày giờ phù hợp như `LocalDate` và `LocalDateTime`.
8. Sử dụng transaction khi nhiều thao tác dữ liệu cần bảo đảm tính nguyên vẹn.
9. Viết code dễ đọc, dễ kiểm thử và dễ bảo trì.
10. Không thêm design pattern hoặc abstraction phức tạp khi chưa có nhu cầu thực tế.

Không tạo hàng loạt class từ đầu. Chỉ bổ sung những thành phần cần thiết cho giai đoạn đang được phê duyệt.

## 7. Bảo mật dữ liệu nhiều người dùng

Đây là yêu cầu quan trọng nhất của Expense Manager.

Mỗi tài khoản chỉ được phép truy cập và thao tác trên dữ liệu thuộc chính tài khoản đó.

### 7.1. Xác thực

- Sử dụng Spring Security.
- Ưu tiên session và cookie khi frontend và backend cùng origin, trừ khi kiến trúc thực tế yêu cầu lựa chọn khác.
- Xử lý CSRF đúng cách đối với các request thay đổi dữ liệu sử dụng session cookie.
- Không tắt CSRF toàn bộ chỉ để giải quyết lỗi tích hợp frontend.
- Mật khẩu phải được băm bằng cơ chế phù hợp, chẳng hạn BCrypt thông qua password encoder của Spring Security.
- Không lưu hoặc trả mật khẩu dạng văn bản thuần.
- Không để lộ password hash hoặc thông tin xác thực trong API response.

### 7.2. Quyền sở hữu dữ liệu

- Không tin tưởng `userId` do frontend gửi lên để xác định chủ sở hữu dữ liệu.
- Xác định người dùng hiện tại từ Security Context phía server.
- Mọi thao tác đọc, sửa và xóa giao dịch đều phải kiểm tra quyền sở hữu.
- Mọi thao tác với danh mục đều phải giới hạn theo người dùng đang đăng nhập.
- Không cho phép người dùng gắn giao dịch của mình vào danh mục của người khác.
- Dashboard chỉ được tổng hợp dữ liệu của người dùng hiện tại.
- Không dựa vào việc ẩn nút trên frontend để bảo vệ tài nguyên.
- Không truy vấn tài nguyên theo ID một cách thiếu kiểm soát rồi trả dữ liệu trước khi xác minh quyền sở hữu.

Ưu tiên truy vấn Repository có điều kiện theo cả ID tài nguyên và chủ sở hữu, hoặc sử dụng phương pháp tương đương có thể kiểm thử rõ ràng.

### 7.3. Kiểm thử bảo mật bắt buộc

Phải kiểm thử các tình huống sau:

1. Người chưa đăng nhập không thể truy cập tài nguyên được bảo vệ.
2. Người dùng A không thể xem giao dịch của người dùng B bằng cách thay ID.
3. Người dùng A không thể sửa hoặc xóa giao dịch của B.
4. Người dùng A không thể xem, sửa hoặc xóa danh mục của B.
5. Người dùng A không thể sử dụng danh mục của B cho giao dịch của mình.
6. Dashboard của A không chứa dữ liệu của B.
7. Việc giả mạo `userId` từ frontend không thể vượt qua cơ chế bảo mật.
8. Đăng nhập, đăng xuất, session và CSRF hoạt động đúng theo cấu hình.

Đây là tiêu chí nghiệm thu bắt buộc, không phải tính năng tùy chọn.

## 8. Quy tắc nghiệp vụ

Tất cả quy tắc quan trọng phải được kiểm tra ở backend.

- Số tiền giao dịch phải lớn hơn 0.
- Loại giao dịch chỉ nhận `INCOME` hoặc `EXPENSE`.
- Loại danh mục phải khớp với loại giao dịch.
- Danh mục và giao dịch phải thuộc người dùng đang đăng nhập.
- Email phải duy nhất.
- Không xóa danh mục đang được giao dịch sử dụng nếu chưa có quy tắc xử lý an toàn được phê duyệt.
- Thống kê theo tháng phải giới hạn đúng khoảng thời gian.
- Mọi truy vấn thống kê phải được giới hạn theo chủ sở hữu dữ liệu.
- Chênh lệch thu chi là tổng thu trừ tổng chi trong kỳ được chọn; không mặc định gọi đó là số dư ngân hàng.

Frontend có thể kiểm tra dữ liệu để cải thiện trải nghiệm người dùng, nhưng không được thay thế việc kiểm tra ở backend.

## 9. Quy tắc thiết kế REST API

Nếu dự án chưa có quy ước API, ưu tiên tiền tố `/api/v1`.

Các nhóm API chính:

- `/api/v1/auth`
- `/api/v1/categories`
- `/api/v1/transactions`
- `/api/v1/dashboard`

Quy tắc:

- Thiết kế endpoint nhất quán với REST.
- Dùng DTO cho request và response.
- Xác thực dữ liệu đầu vào.
- Phân trang và lọc dữ liệu khi cần.
- Trả mã HTTP phù hợp.
- Sử dụng định dạng lỗi nhất quán.
- Không để lộ stack trace, cấu trúc nội bộ hoặc thông tin nhạy cảm.
- Không thay đổi API đã có mà không giải thích ảnh hưởng.

Các mã HTTP tham khảo:

- `400`: dữ liệu đầu vào không hợp lệ.
- `401`: chưa xác thực.
- `403`: không được phép thực hiện thao tác.
- `404`: không tìm thấy tài nguyên hoặc tài nguyên không được phép hiển thị theo chính sách đã chọn.
- `409`: dữ liệu bị xung đột, chẳng hạn email đã tồn tại.

Phải giữ cách xử lý nhất quán và tránh làm lộ sự tồn tại của dữ liệu riêng tư thông qua thông báo lỗi.

## 10. Quy trình kiểm thử

Sau mỗi phần triển khai, hãy chọn và thực hiện những kiểm thử phù hợp:

- Unit test cho nghiệp vụ.
- Repository test cho truy cập dữ liệu.
- Integration test cho API và database.
- Security test cho xác thực, phân quyền và quyền sở hữu dữ liệu.
- Kiểm tra frontend khi có thay đổi giao diện hoặc tích hợp API.

Ưu tiên sử dụng công cụ kiểm thử đã có trong dự án. Nếu cần thêm dependency, hãy giải thích lý do và xin phê duyệt khi thay đổi đó ảnh hưởng đáng kể đến cấu hình.

Sau khi triển khai:

1. Chạy các bài kiểm thử liên quan.
2. Chạy build nếu môi trường cho phép.
3. Đọc lại diff để phát hiện thay đổi ngoài phạm vi.
4. Kiểm tra lỗi bảo mật và nguy cơ hồi quy.
5. Báo cáo kết quả thực tế.

Không được nói test thành công nếu chưa chạy hoặc chưa có kết quả xác minh.

Nếu không thể kiểm thử do SQL Server chưa sẵn sàng, môi trường thiếu dependency hoặc nguyên nhân khác, hãy nêu rõ phần chưa kiểm chứng và lý do.

## 11. Quy tắc Git và GitHub

- Kiểm tra nhánh hiện tại và working tree trước khi làm việc.
- Không phát triển trực tiếp trên `main`.
- Sử dụng feature branch cho các tính năng riêng biệt.
- Không làm mất các thay đổi chưa commit của người dùng.
- Không tự ý sử dụng `reset --hard`, force-push hoặc lệnh xóa dữ liệu.
- Không thay đổi branch protection.
- Không tự commit hoặc push nếu người dùng chưa yêu cầu.
- Không đưa secret hoặc dữ liệu tài chính thật vào repository.
- Chỉ thay đổi những file thuộc phạm vi đã được đồng ý.

Tên nhánh gợi ý:

- `feature/database-model`
- `feature/authentication`
- `feature/categories`
- `feature/transactions`
- `feature/dashboard`
- `feature/frontend`
- `test/security-hardening`

## 12. Quy trình thay đổi cơ sở dữ liệu

Trước khi thay đổi schema:

1. Kiểm tra Entity và cấu trúc database hiện tại.
2. Xác định schema đã được quản lý bằng công cụ migration hay chưa.
3. Giải thích các bảng, cột, quan hệ hoặc ràng buộc cần thay đổi.
4. Bảo toàn dữ liệu đang có.
5. Không thực hiện thao tác phá hủy dữ liệu.
6. Kiểm thử thay đổi trong môi trường phát triển an toàn.

Nếu cần migration, hãy đề xuất một giải pháp phù hợp như Flyway và giải thích cách tích hợp trước khi thực hiện.

Không kết hợp việc tự động thay đổi schema của Hibernate với migration một cách thiếu kiểm soát.

Không chạy lệnh xóa bảng, xóa database hoặc tạo lại schema trên môi trường có thể chứa dữ liệu người dùng nếu chưa được phê duyệt rõ ràng.

## 13. Quy tắc phát triển frontend

- Sử dụng HTML có cấu trúc rõ ràng.
- Tách CSS và JavaScript hợp lý.
- Có trạng thái đang tải, dữ liệu rỗng, thành công và lỗi.
- Hiển thị thông báo validation dễ hiểu.
- Không lưu mật khẩu hoặc thông tin xác thực nhạy cảm trong frontend.
- Không dùng kiểm tra quyền ở frontend làm biện pháp bảo mật duy nhất.
- Xử lý session hết hạn và lỗi xác thực đúng cách.
- Không để nội dung do người dùng nhập được thực thi như HTML hoặc JavaScript.
- Tích hợp request với cơ chế session và CSRF của backend.

Không tự ý thêm frontend framework nếu người dùng chưa yêu cầu và chưa có lý do kiến trúc thuyết phục.

## 14. Lộ trình triển khai Expense Manager

Triển khai theo từng giai đoạn, không làm toàn bộ ứng dụng trong một lần.

### Giai đoạn 0 — Khảo sát

- Đọc repository.
- Kiểm tra cấu hình.
- Xác định phần đã có và còn thiếu.
- Đề xuất kế hoạch.
- Chờ người dùng phê duyệt.

### Giai đoạn 1 — Mô hình dữ liệu

- Kiểm tra Entity và quan hệ.
- Hoàn thiện Repository.
- Xác định ràng buộc dữ liệu.
- Kiểm tra kết nối SQL Server.
- Bảo toàn dữ liệu hiện tại.

### Giai đoạn 2 — Xác thực và bảo mật

- Đăng ký.
- Đăng nhập và đăng xuất.
- Băm mật khẩu.
- Cấu hình Spring Security.
- Xử lý session và CSRF.
- Kiểm thử bảo mật nhiều người dùng.

### Giai đoạn 3 — Quản lý danh mục

- Tạo danh mục.
- Xem danh sách danh mục.
- Sửa danh mục.
- Xóa danh mục theo quy tắc an toàn.
- Kiểm tra quyền sở hữu.

### Giai đoạn 4 — Quản lý giao dịch

- Tạo giao dịch thu chi.
- Xem lịch sử giao dịch.
- Sửa và xóa giao dịch.
- Lọc theo tháng và loại giao dịch.
- Phân trang khi cần.
- Kiểm tra nghiệp vụ và quyền sở hữu.

### Giai đoạn 5 — Dashboard

- Tổng thu trong kỳ.
- Tổng chi trong kỳ.
- Chênh lệch thu chi.
- Thống kê theo danh mục.
- Kiểm tra giới hạn dữ liệu theo tài khoản.

### Giai đoạn 6 — Frontend

- Đăng ký và đăng nhập.
- Dashboard.
- Danh sách giao dịch.
- Form thêm và sửa giao dịch.
- Quản lý danh mục.
- Tích hợp API.
- Xử lý trạng thái tải và lỗi.

### Giai đoạn 7 — Hoàn thiện

- Chạy test suite.
- Rà soát bảo mật.
- Kiểm tra cấu hình.
- Sửa lỗi phát hiện được.
- Cập nhật README.
- Hướng dẫn chạy ứng dụng.
- Đề xuất hướng phát triển tiếp theo.

Không tự động chuyển qua toàn bộ các giai đoạn. Hãy hoàn thành giai đoạn đang được phê duyệt, báo cáo và chờ người dùng xác nhận bước tiếp theo.

## 15. Cách làm việc trong từng nhiệm vụ

Với mỗi nhiệm vụ đã được phê duyệt:

1. Tóm tắt mục tiêu.
2. Đọc các file liên quan.
3. Giải thích cách triển khai dự kiến.
4. Liệt kê các file cần tạo hoặc sửa.
5. Thực hiện đúng phạm vi được giao.
6. Chạy kiểm thử phù hợp.
7. Đọc lại diff.
8. Báo cáo kết quả và rủi ro còn tồn tại.

Nếu yêu cầu chưa rõ, hãy hỏi một câu cụ thể thay vì tự suy đoán quyết định nghiệp vụ quan trọng.

Nếu phát sinh lỗi, hãy tìm nguyên nhân trước khi sửa. Không tắt bảo mật, bỏ validation hoặc làm suy yếu test chỉ để khiến build thành công.

Hãy giải thích các quyết định kỹ thuật theo cách giúp người dùng hiểu được Spring Boot, đồng thời tận dụng kiến thức Java Jakarta EE mà người dùng đã có.

## 16. Mẫu báo cáo bắt buộc

Sau mỗi giai đoạn hoặc nhiệm vụ, hãy báo cáo bằng tiếng Việt theo cấu trúc:

### 1. Mục tiêu
Nhiệm vụ này giải quyết vấn đề gì?

### 2. Các thay đổi
Những file đã tạo, sửa hoặc xóa và vai trò của chúng.

### 3. Quyết định kỹ thuật
Giải thích những lựa chọn quan trọng và lý do.

### 4. Kiểm thử
Các lệnh đã chạy, kết quả thực tế, test thất bại và test chưa thể thực hiện.

### 5. Bảo mật
Những biện pháp đã triển khai, kết quả kiểm chứng và rủi ro còn lại.

### 6. Trạng thái Git
Nhánh hiện tại và tình trạng các thay đổi chưa commit.

### 7. Bước tiếp theo
Đề xuất nhiệm vụ tiếp theo và các quyết định cần người dùng đưa ra.

### 8. Chờ phê duyệt
Dừng tại phạm vi đã hoàn thành. Không tự động triển khai giai đoạn tiếp theo khi chưa được đồng ý.

## 17. Chỉ dẫn bắt đầu

Khi bắt đầu làm việc với Expense Manager, hãy đọc file `CLAUDE.md` này trước.

Nếu đây là lần đầu khảo sát dự án hoặc bắt đầu một công việc mới, hãy thực hiện Giai đoạn 0:

- Chỉ đọc và phân tích repository.
- Không sửa file.
- Không cài hoặc nâng cấp dependency.
- Không thay đổi cấu hình.
- Không chỉnh sửa database.
- Không chạy lệnh có khả năng phá hủy dữ liệu.

Sau đó báo cáo tình trạng dự án, đề xuất kế hoạch triển khai và chờ người dùng phê duyệt.

Với phiên làm việc đang tiếp diễn, hãy kiểm tra trạng thái thực tế và chỉ tiếp tục trong phạm vi nhiệm vụ đã được đồng ý gần nhất.
