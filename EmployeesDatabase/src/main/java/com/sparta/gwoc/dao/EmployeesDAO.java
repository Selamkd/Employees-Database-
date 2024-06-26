package com.sparta.gwoc.dao;

import com.sparta.gwoc.dto.Employee;
import com.sparta.gwoc.dto.EmployeeFactory;
import com.sparta.gwoc.utils.LoggerUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EmployeesDAO implements DAOInterface {
    public static Connection dbConnection;
    private static final Logger logger = LoggerUtil.getLogger(EmployeesDAO.class.getName());

    @Override
    public void openDBConnection() {
        DatabaseProperties properties = DatabaseProperties.loadPropertiesFromFile();
        ConnectionManager connectionManager = new ConnectionManager(properties);
        dbConnection = connectionManager.getConnection();
    }


    @Override
    public List<Employee> getAllEmployeeRecords() {
        List<Employee> employeeList = new ArrayList<>();

        try (Statement statement = dbConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(PreparedStatements.GET_ALL_EMPLOYEES)) {
            logger.fine("Executing statement: " + PreparedStatements.GET_ALL_EMPLOYEES);
            while (resultSet.next()) {
                Employee employee = resultSetToEmployee(resultSet);
                employeeList.add(employee);
            }
        } catch (SQLException e) {
            logger.warning("SQL Statement Execution Failed" + DAOLoggingUtils.logSQLException(e));
        }
        logger.fine("Retrieved records for " + employeeList.size() + " employees");
        return employeeList;
    }


    private Employee resultSetToEmployee(ResultSet resultSet) throws SQLException {
        return new Employee(
                resultSet.getString(1),
                resultSet.getString(2),
                resultSet.getString(3),
                resultSet.getString(4).charAt(0),
                resultSet.getString(5),
                resultSet.getString(6).charAt(0),
                resultSet.getString(7),
                resultSet.getDate(8).toLocalDate(),
                resultSet.getDate(9).toLocalDate(),
                resultSet.getInt(10)
        );
    }


    @Override
    public Employee getEmployeeRecordByID(String id) {
        Employee employee = null;

        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(PreparedStatements.GET_EMPLOYEE_BY_ID)) {
            preparedStatement.setString(1, id);
            logger.fine("Executing query: " + preparedStatement);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                employee = resultSetToEmployee(resultSet);
            }
        } catch (SQLException e) {
            logger.warning("SQL Statement Execution Failed" + DAOLoggingUtils.logSQLException(e));
        }
        logger.fine("Employee found: " + (employee == null ? "no employee found" : employee.empID()));
        return employee;
    }


    @Override
    public int deleteEmployeeRecordByID(String id) {
        int recordsUpdated = 0;
        try (PreparedStatement preparedStatements = dbConnection.prepareStatement(PreparedStatements.DELETE_EMPLOYEE_BY_ID)) {
            preparedStatements.setString(1, id);
            logger.fine("Executing query: " + preparedStatements);
            recordsUpdated = preparedStatements.executeUpdate();

        } catch (SQLException e) {
            logger.warning("SQL Statement Execution Failed" + DAOLoggingUtils.logSQLException(e));
        }
        logger.fine("Deleted " + recordsUpdated + " employee records.");
        return recordsUpdated;
    }


    @Override
    public int insertEmployees(List<Employee> employeeList) {
        int recordsInserted = 0;
        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(PreparedStatements.INSERT_EMPLOYEES)) {
            for (Employee employee : employeeList) {
                prepareInsertStatement(employee, preparedStatement);
                logger.finer("Executing query: " + preparedStatement);
                recordsInserted += attemptInsert(preparedStatement);
            }
        } catch (SQLException e) {
            logger.warning("SQL Statement Execution Failed" + DAOLoggingUtils.logSQLException(e));
        }
        logger.fine("Inserted " + recordsInserted + " employee records.");
        return recordsInserted;
    }

    private void prepareInsertStatement(Employee employee, PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setString(1, employee.empID());
        preparedStatement.setString(2, employee.prefix());
        preparedStatement.setString(3, employee.firstName());
        preparedStatement.setString(4, String.valueOf(employee.middleInitial()));
        preparedStatement.setString(5, employee.lastName());
        preparedStatement.setString(6, String.valueOf(employee.gender()));
        preparedStatement.setString(7, employee.email());
        preparedStatement.setDate(8, Date.valueOf(employee.dateOfBirth()));
        preparedStatement.setDate(9, Date.valueOf(employee.dateOfJoin()));
        preparedStatement.setInt(10, employee.salary());
    }

    private int attemptInsert(PreparedStatement preparedStatement) {
        int rowsAffected = 0;
        try {
            rowsAffected = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to insert record" + DAOLoggingUtils.logSQLException(e));
        }
        return rowsAffected;
    }


    @Override
    public int updateFirstNameById(String id, String newFirstName) {
        int rowsAffected = 0;
        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(PreparedStatements.UPDATE_FIRSTNAME_BY_ID)) {
            preparedStatement.setString(1, newFirstName);
            preparedStatement.setString(2, id);
            logger.fine("Executing query: " + preparedStatement);
            rowsAffected = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.warning("SQL Statement Execution Failed" + DAOLoggingUtils.logSQLException(e));
        }
        logger.fine("Updated " + rowsAffected + " employee records.");
        return rowsAffected;
    }

    @Override
    public int countTheNumberOfEmployees() {
        int totalCount  =0;

        try (Statement statement = dbConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(PreparedStatements.GET_COUNT_OF_ALL_EMPLOYEES)) {
            logger.fine("Executing query: " + PreparedStatements.GET_COUNT_OF_ALL_EMPLOYEES);
            while (resultSet.next()) {
                totalCount = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            logger.warning("SQL Statement Execution Failed" + DAOLoggingUtils.logSQLException(e));
        }
        return totalCount;
}

    @Override
    public void closeDBConnection() {
        try {
            dbConnection.close();
            logger.info("Database connection closed successfully.");
        } catch (SQLException e) {
            logger.warning("Unable to close database connection."
                            + DAOLoggingUtils.logSQLException(e));
        }
    }


    /*
     * This method is using for manual testing of the EmployeeDAO class in the absence
     * of automated integration testing.
     */
    public static void main(String[] args) throws SQLException {
        EmployeesDAO dao = new EmployeesDAO();
        dao.openDBConnection();
        List<Employee> employees = EmployeeFactory.getValidEmployees();
        dao.insertEmployees(employees);

        // Attempting to get and delete non-existent IDs does not crash the program
        dao.getEmployeeRecordByID("000001");
        dao.deleteEmployeeRecordByID("000001");

        // Verify update changes Raymundo -> Raymond
        dao.updateFirstNameById("191728", "Raymond");
        Employee raymond = dao.getEmployeeRecordByID("191728");
        System.out.println(raymond);

        // Number of employees should be 9911 (verified by querying database directly)
        System.out.println("Number of employees: " + dao.countTheNumberOfEmployees());

        // Deleting an existing record decreases the count by one
        dao.deleteEmployeeRecordByID("111498");
        System.out.println("Number of employees after deleting one record: " + dao.countTheNumberOfEmployees());

        dao.closeDBConnection();

    }


}

