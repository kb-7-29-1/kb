package org.scoula.persistence;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.fail;

@Log4j2
public class JDBCTests {

    @BeforeAll //아래에 테스트할 메서드 실행전에 무조건 실행하는 기능
    public static void setup() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConnection(){
        String url= "jdbc:mysql://localhost:3306/scoula_db";
        //네트워크로 연결할 때는 예외처리해주고, 연결끊어주어야함.
        try(Connection con = DriverManager.getConnection(url, "scoula", "1234")) {
            log.info(con);
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }



}
