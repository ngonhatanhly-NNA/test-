from app.utils.files import sanitize_filename


def test_sanitize_filename():
    assert sanitize_filename("../../etc/passwd.pdf") == "passwd.pdf"
    assert sanitize_filename("C:\\Users\\a\\Đề Lý 10.pdf") == "Đề Lý 10.pdf"
    assert sanitize_filename("a<b>|c?.pdf") == "a_b__c_.pdf"
    assert sanitize_filename(None) == "upload.pdf"
    assert len(sanitize_filename("x" * 300 + ".pdf")) <= 100
