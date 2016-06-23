package org.jboss.bpm.console.server.util;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import junit.framework.TestCase;

import java.util.Date;
import java.util.Map;

public class ProcessHistoryQueryDaoTest extends TestCase {

    private MysqlDataSource dataSource;

    public void setUp() {
        dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://172.20.0.40:3306/vistobpm_teste");
        dataSource.setUser("jbpm");
        dataSource.setPassword("jbpm");
    }

    public void testQuery() {

        final ProcessHistoryQueryDao dao = new ProcessHistoryQueryDao(dataSource);
        int count = 0;
        final Map<Long, Map<String, Object>> result = dao.queryFinishedProcessHistory(
                "someProcessName",
                "Aprovado",
                "someFileName",
                new Date().getTime(),
                new Date().getTime(),
                new Date().getTime(),
                new Date().getTime(),
                new String[]{}, new String[] {},
                new String[] {
                "boticario.dam.aprovacao.folheteria.v1",
                "boticario.dam.aprovacao.folheteria.v1",
                "boticario.dam.aprovacao.clube.evolucao.v1",
                "boticario.dam.aprovacao.upload.v2",
                "boticario.dam.aprovacao.pdv.pecas.v1",
                "boticario.dam.aprovacao.produtos.v1",
                "boticario.dam.aprovacao.instore.radiotv.v1",
                "boticario.dam.aprovacao.midia.regional.v1",
                /*"boticario.dam.aprovacao.loja.bolsa.v1",*/
                "boticario.dam.aprovacao.pessoas.v1"
        }, new String[] { "r_creator", "r_assetId" },
                0L, 100L, null
        );
        for (Map.Entry<Long, Map<String, Object>> row : result.entrySet()) {
            for (Map.Entry<String,Object> field : row.getValue().entrySet()) {
                System.out.println(field.getKey() + ": " + field.getValue());
            }
            System.out.println("========================== " + ++count);
        }

    }

}
