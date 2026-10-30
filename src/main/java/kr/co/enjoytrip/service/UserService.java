package kr.co.enjoytrip.service;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.regex.Pattern;
import kr.co.enjoytrip.dao.UserDao;
import kr.co.enjoytrip.dao.HotPlaceDao;
import kr.co.enjoytrip.dao.UserDao.ResetRequest;
import kr.co.enjoytrip.dto.UserDto;

public class UserService {
    private static final Pattern LOGIN = Pattern.compile("[a-zA-Z0-9_]{4,50}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final UserDao dao = new UserDao();

    public void register(String login, String email, String name, String password) throws SQLException {
        if (login == null || !LOGIN.matcher(login).matches()) throw new ValidationException("아이디는 영문·숫자·밑줄 4~50자로 입력하세요.");
        validateProfile(email, name);
        validatePassword(password);
        if (dao.findByLogin(login) != null || dao.findByEmail(email) != null) throw new ValidationException("이미 사용 중인 아이디 또는 이메일입니다.");
        try { dao.insert(login, email, name.trim(), PasswordHasher.hash(password), "USER"); }
        catch (SQLIntegrityConstraintViolationException e) { throw new ValidationException("이미 사용 중인 아이디 또는 이메일입니다."); }
    }

    public UserDto authenticate(String login, String password) throws SQLException {
        UserDto user = login == null ? null : dao.findByLogin(login);
        if (user == null || password == null || !PasswordHasher.verify(password, user.passwordHash()))
            throw new ValidationException("아이디 또는 비밀번호를 확인하세요.");
        return user;
    }

    public UserDto find(long id) throws SQLException { return dao.findById(id); }

    public void update(long id, String email, String name) throws SQLException {
        validateProfile(email, name);
        UserDto other = dao.findByEmail(email);
        if (other != null && other.id() != id) throw new ValidationException("이미 사용 중인 이메일입니다.");
        try { dao.update(id, email, name.trim()); }
        catch (SQLIntegrityConstraintViolationException e) { throw new ValidationException("이미 사용 중인 이메일입니다."); }
    }

    public void changePassword(long id, String oldPassword, String newPassword) throws SQLException {
        UserDto user = dao.findById(id);
        if (user == null || !PasswordHasher.verify(oldPassword, user.passwordHash()))
            throw new ValidationException("현재 비밀번호가 맞지 않습니다.");
        validatePassword(newPassword);
        dao.updatePassword(id, PasswordHasher.hash(newPassword), false);
    }

    public void delete(long id, String password) throws SQLException {
        UserDto user = dao.findById(id);
        if (user == null || user.admin() || !PasswordHasher.verify(password, user.passwordHash()))
            throw new ValidationException("탈퇴할 수 없습니다. 비밀번호를 확인하세요.");
        List<String> images=new HotPlaceDao().imagePathsForUser(id);
        dao.delete(id);
        new HotPlaceService().deleteUserPhotos(images);
    }

    public void requestReset(String email) throws SQLException {
        if (email == null || !EMAIL.matcher(email).matches()) throw new ValidationException("이메일 형식을 확인하세요.");
        UserDto user = dao.findByEmail(email);
        if (user != null) dao.requestReset(user.id());
    }

    public List<ResetRequest> pendingResets() throws SQLException { return dao.pendingResets(); }

    public String completeReset(long requestId, long adminId) throws SQLException {
        UserDto admin = dao.findById(adminId);
        if (admin == null || !admin.admin()) throw new ValidationException("관리자 권한이 필요합니다.");
        String temp = PasswordHasher.temporaryPassword();
        if (!dao.completeReset(requestId, adminId, PasswordHasher.hash(temp)))
            throw new ValidationException("처리할 수 없는 요청입니다.");
        return temp;
    }

    private void validateProfile(String email, String name) {
        if (email == null || email.length() > 255 || !EMAIL.matcher(email).matches()) throw new ValidationException("이메일 형식을 확인하세요.");
        if (name == null || name.isBlank() || name.trim().length() > 80) throw new ValidationException("이름은 1~80자로 입력하세요.");
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 128)
            throw new ValidationException("비밀번호는 10~128자로 입력하세요.");
    }
}
