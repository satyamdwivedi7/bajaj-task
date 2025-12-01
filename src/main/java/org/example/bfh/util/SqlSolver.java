package org.example.bfh.util;

public class SqlSolver {
    public static String solveFromQuestionText(String questionText, String regNo) {

        String finalQuery =
                "SELECT \n" +
                        "    d.DEPARTMENT_NAME,\n" +
                        "    AVG(TIMESTAMPDIFF(YEAR, e.DOB, CURRENT_DATE)) AS AVERAGE_AGE,\n" +
                        "    SUBSTRING_INDEX(\n" +
                        "        GROUP_CONCAT(CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME)\n" +
                        "            ORDER BY e.FIRST_NAME, e.LAST_NAME SEPARATOR ', '),\n" +
                        "        ', ',\n" +
                        "        10\n" +
                        "    ) AS EMPLOYEE_LIST\n" +
                        "FROM EMPLOYEE e\n" +
                        "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID\n" +
                        "JOIN PAYMENTS p ON p.EMP_ID = e.EMP_ID\n" +
                        "WHERE p.AMOUNT > 70000\n" +
                        "GROUP BY d.DEPARTMENT_ID, d.DEPARTMENT_NAME\n" +
                        "ORDER BY d.DEPARTMENT_ID DESC;";

        return finalQuery;
    }
}
