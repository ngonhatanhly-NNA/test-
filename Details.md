MODEL
Entity(id, createdAt, updatedAt)
	->User(username, passwordHash, email, fullname, phoneNumber, address, status)
		-> Bidder(walletBalance, creditcardInfo)
			-> Seller(upgraded through Bidder) (shopName, rating, totalReviews, backAccountNumber, isVerified)
		-> Admin(roleLevel, látLoginIP)
		
		

Client 
	- ClientApp: là phần màn hình chờ đầu tiên chuyển cảnh đến Login và Register 
	- LoginController: xử lí các logic trên client và kết nối với DB để đăng nhập vào đúng dữ liệu người dùng
	- RegisterController: xử lí register, khi xử lí xong truyển quá Socket, Response, Request để đến Service
	- DashboardController: Là dashboard gồm các mục chính

	- RequestSender: 

	- SceneController: hàm mang logic chuyển cảnh chung cho các Scene
	- SmallAnimation: dùng ImageView đ cắt ảnh cho như chuyển động thật, cùng hàm xử lí việc cắt ảnh


ServerApp (vòng lặp while để chấp nhận kết nối từ client, khi nhận đc Object từ client, đẩy nó vào UserService để xử lí)
		
Service
* config:
* * DBConnection: hàm chính để liên kết scheme db của toàn hệ thống, ử dụng Singleton để tạo duy nhất 1 database 
*Repository
** UserRepository: Khi người dùng đăng kí, sẽ lấy 4 thông tin chính là userName, fullName, passWord, email
**      			status: Server tự động gán trong DAO, createdAt/updatedAt tự động nhờ lệnh tỏng mySQL, address, phoneNumber: tự động update trong Edit Profile
** Cơ chế hoạt động của User Repo: Có 2 hàm là saveUsers và getUsername: cả 2 tuân thủ Singleton (Cho đối tượng Connection chỉ tạo duy nhất 1 luồng) + try-with-resources cho patternStatement để đóng ngay sau khi dùng xong. Hàm save_users có thêm 3 hàm save theo từng Role theo ngtac ISP trong SOLID)
- Đa hình giúp từ Users có thể downcasting sang các role con, sẵn sàng mở rộng, tránh chính sửa
** Cơ chế tạo bảng DB cho User: Tạo 4 bảng trong đó bảng chính là Users(Khóa chính là cột ID), 3 bảng phụ với 3 roles là bidder, seller, admin (Khóa chính kiếm khóa ngoại là cột user_id) -> Hiệu suất đăng nhập + truy xuất dữ liệu acc mạnh hơn
-> Mỗi khi đăng nhập chỉ cần tìm thông tin acc dựa theo khóa chính, lần theo cột Role để tìm chính xác bảng cần lấy các thông tin riêng cho role đó
**  - chưa có Repo, mới là hàm check
*Service
**UserService: Xử lí các logic kiểm tra xem username đã tồn tại hay chưa
* * AuthService: Xử lí các hàm login, Register giao tiếp qua HTTP (sử dụng REST API, Json) đề xử lí giao tiếp và truyền chuỗi gián tiếp lưu vào DB

**MODEL - cacs model đều cần các constructor rỗng để làm việc với Socket, chuẩn bị cho logic Login, register và DB
*** Entity (Vật thể định nghĩa chung, cha của Users và Item): id, createdAt, updatedAt: tự tạo id cho user và cập nhật thời gian
**** User(username, passwordHash, email, fullname, phoneNumber, address, ,status){Các thông tin cơ bản chung của User, không thể đổi userName, và passWord sẽ đc trả về Hash}
***** Bidder (walletBalance, creditcardInfo) {tiền mặc định ban đầu là 0, creditCar tự cấp nhật ở phần profile, ở setter, chỉ có thể thêm hoạc bớt tiền,k đc 1 mạch set tiền về giá trị này luôn}
*****  	-> Seller (con của Bidder, do đề yêu cầu Bidder đc upgraded lên Seller) (thêm shopname, rating, totalReviews, bankAccount, và isVerified)
***** Admin(là người điều hành) (roleLevel, látLoginIP)
**** Item(thông tin cơ bản của các Item)

* *Luồng chạy của Bidding_Server (3 tầng MVC - Controller, Repo, DAO)*: 
  * Tầng Controller (Giao tiếp / Network): Đứng ở cửa đón khách. Nhận tín hiệu HTTP, lấy chuỗi văn bản JSON ném vào cái "máy xay" GSON để đúc thành Object Java.
  * Tầng Service (Não bộ / Business Logic): Nhận Object Java từ Controller. Nơi này KHÔNG quan tâm đến mạng Internet, cũng KHÔNG quan tâm đến Database. Nó chỉ thuần túy chạy thuật toán Java (kiểm tra điều kiện, tính toán giá, v.v.). Xử lý xong, nó ném Object xuống cho tầng dưới.
  * Tầng Repository/DAO (Kho bãi / Database): Nhận Object Java từ Service, dùng cái cầu nối JDBC để dịch Object đó thành lệnh SQL, rồi cắm ống nước chui xuống MySQL cất dữ liệu.

*Shared
**dot -- các dữ liệu client truyền đi và server gửi v có tính bảo mật, không lộ hoàn toàn thông tin ật khẩu user 

// Tại sao lai cần network, k thể tin User để cho trực tiếp dlieu và DB cho nó check, vì lộ 100% mật khẩu, 
// Users có thể tải phần mềm dịch ngược Decompiler để đọc mật khẩu MySQL 
// Client thuộc máy User -> dễ dang hack, DB là người gác cổng, k cho hack ở Server chính
// Quá tải Database, nối 10000 Users vào DB, sẽ bị sập
**network(Chịu trách nhiệm giao tiếp giữa Cient và Server)
***Khi có request từ Client , nó băm object thành byte truyền qua manjc và server sẽ nhận đống đó


Lỗi kết nối server do chuaw cos Serializable cho cas Model, k the truyen mang dc, loi ket noi DB 
SERVER_PORT mangj khacs db 3306, sua vao loi db :">>




Details về cách run của trình đang ký - đăng nhập:
- Ở RegisterController, khi người dùng đăng ký thông tin, các thông tin hiện được record ở trang đăng ký được Gson chuyển sang chuỗi Json v HTTP Request truyền gửi dữ liệu đi, 
- HTTP Response sẽ đợi câu trả lời rồi theo đó mới xem minh nên làm gì (try - catch or ì else ), dữ liệu gửi đi được truyền xuống AuthService, xủ lí các dữ liệu về đăng ký, đăng nhâp
- AuthService sẽ mở json, truyền dữ liệu vào model gốc ở server (các model ở client và server phải có chức năng khác nhau chút vf tính bảo mật), rồi gọi UserRepository, mở DBConnection xem truyenf vào DB đc k
- tượng tự với đăng nhập, nhận, hỏi db, check và trả lại


* [**Phân cấp Người dùng**: Lớp trừu tượng `User` là lớp cha của các lớp `Admin`, `Bidder`, và `Seller`.
      * lớp User :
      * thuộc tính:
          * Thông tin tài khoản: username (định danh duy nhất), passwordHash (mật khẩu đã mã hóa).
          * Thông tin cá nhân: fullName, email, phoneNumber, address.
          * Trạng thái tài khoản: status (Quản lý các trạng thái ACTIVE, INACTIVE, BANNED).
      * phương thức:
        * Hàm khởi tạo (User Constructor): Tiếp nhận id và truyền lên lớp cha thông qua super(id), đồng thời thiết lập các thông tin cơ bản cho người dùng.
        * Nhóm Getter (Truy vấn dữ liệu): Cung cấp khả năng đọc thông tin như getUsername(), getEmail(), getStatus(). Riêng username chỉ có Getter để đảm bảo tính bất biến.
        * Nhóm Setter & Update (Thay đổi dữ liệu): * setEmail(), setFullName(), setAddress(): Cập nhật thông tin cá nhân.
          * updatePassword(String): Cập nhật mật khẩu mới dưới dạng chuỗi đã băm.
          * updateStatus(String): Cho phép thay đổi trạng thái hoạt động (thường do Admin điều khiển).
          * Cơ chế Tự động Cập nhật (updateTimestamp): Mọi phương thức thay đổi dữ liệu (Setter/Update) đều tự động gọi hàm này để ghi lại thời điểm tương tác cuối cùng, đảm bảo tính minh bạch của dữ liệu.
      * lớp Seller:
        * thuộc tính:
          * Thông tin cửa hàng: shopName (Tên hiển thị thương hiệu), bankAccountNumber (Số tài khoản nhận tiền thanh toán).
          * Chỉ số uy tín (Rating): rating (Điểm trung bình từ 1.0 - 5.0 sao), totalReviews (Tổng số lượt đánh giá đã nhận).
          * Kiểm soát quyền hạn: isVerified (Trạng thái xác minh bởi Admin; mặc định là false)
        * phương thức:
          * Nâng cấp tài khoản: Seller(oldBidder, ...) – Cho phép chuyển đổi từ người mua (Bidder) sang người bán mà không làm mất số dư ví hay thông tin cá nhân cũ.
          * Quản lý uy tín: addReviewScore(score) – Thuật toán tự động tính toán lại điểm trung bình cộng tích lũy mỗi khi có khách hàng đánh giá mới.
          * Cập nhật thông tin: setShopName(), setBankAccountNumber() – Thay đổi thông tin cửa hàng và tự động đồng bộ thời gian chỉnh sửa (updateTimestamp)
* NOTE(Nhật ký fix bugs lúc 11h40 ngày 5/4): Quy tắc Tối thượng về OOP (Lớp User): 
* > Khi cập nhật thông tin chung (email, sđt, địa chỉ), bắt buộc phải thao tác trên model cha là User. 
  > Tôi đã sửa lại UserService (trước đó bị ép kiểu cứng thành Bidder). Nếu giữ nguyên, một ông Admin vào đổi số điện thoại sẽ bị hệ thống âm thầm giáng cấp xuống làm Bidder, hỏng bét logic phân quyền của anh em mình.
* Sau khi fix bugs ngày 6/4 thì:
* > AE chú ý code AI hay gì thì phải prompt đủ ngữ cảnh, lấy đúng khu vực code cần thiết để AI code chính xác
  > Code AI cũng phải mở model hay các class liên quan để còn biết đúng sai. 
  > Mấy ngày nay có tình trạng copy code xong không check, AI nó bịa thuộc tính, bịa tên hàm cũng chép (Rồi thậm chí copy code còn copy lệch, gây lỗi khai báo lặp hàm và làm t tìm mãi mới thấy)