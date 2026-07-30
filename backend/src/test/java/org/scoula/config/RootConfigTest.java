package org.scoula.config;

import com.salgosipo.global.config.RootConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= {RootConfig.class})
@Log4j2
class RootConfigTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    @DisplayName("hikariCP test결과 확인")
    public void dataSource() throws SQLException {
        try(Connection con = dataSource.getConnection()){
            //hikariCP에 close할때는 안쓰는 것으로 등록!!!
            log.info("hikariCP 준비 완료.");
            log.info(con);
        }
    }

    @Test
    public void testSqlSessionFactory() {
        try (
                SqlSession session = sqlSessionFactory.openSession(); // SqlSession을 직접 열어 SQL 실행 준비
                Connection con = session.getConnection()     // SqlSession 내부의 실제 DB 연결 객체
        ) {
            log.info(session);
            log.info(con);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}