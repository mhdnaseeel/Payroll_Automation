package com.fci.automation;

import com.fci.automation.dto.EmployeeImportSummary;
import com.fci.automation.entity.Employee;
import com.fci.automation.repository.EmployeeRepository;
import com.fci.automation.service.EmployeeImportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeImportServiceTest {

    private StubEmployeeRepository employeeRepository;
    private EmployeeImportService employeeImportService;

    @BeforeEach
    void setUp() {
        employeeRepository = new StubEmployeeRepository();
        employeeImportService = new EmployeeImportService();
        ReflectionTestUtils.setField(employeeImportService, "employeeRepository", employeeRepository);
    }

    private MockMultipartFile createExcelFile(String[][] rowsData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees");
            for (int r = 0; r < rowsData.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rowsData[r].length; c++) {
                    if (rowsData[r][c] != null) {
                        row.createCell(c).setCellValue(rowsData[r][c]);
                    }
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void testImportNewEmployees() throws IOException {
        String[][] data = {
                {"Member ID", "Name", "UAN", "IP Number", "Bank Account", "IFSC", "Category"},
                {"101", "John Doe", "100000000001", "2000000001", "30000000001", "SBIN0001234", "CL"},
                {"102", "Jane Smith", "100000000002", "2000000002", "30000000002", "SBIN0001234", "HL"}
        };

        MockMultipartFile file = createExcelFile(data);
        EmployeeImportSummary summary = employeeImportService.importEmployees(file);

        assertEquals(2, summary.getTotalRecords());
        assertEquals(2, summary.getCreatedCount());
        assertEquals(0, summary.getUpdatedCount());
        assertEquals(0, summary.getSkippedCount());
        assertEquals(0, summary.getFailedCount());
        assertEquals(2, employeeRepository.findAll().size());
    }

    @Test
    void testImportDuplicateFile_Skipped() throws IOException {
        String[][] data = {
                {"Member ID", "Name", "UAN", "IP Number", "Bank Account", "IFSC", "Category"},
                {"101", "John Doe", "100000000001", "2000000001", "30000000001", "SBIN0001234", "CL"}
        };

        MockMultipartFile file1 = createExcelFile(data);
        EmployeeImportSummary summary1 = employeeImportService.importEmployees(file1);
        assertEquals(1, summary1.getCreatedCount());

        MockMultipartFile file2 = createExcelFile(data);
        EmployeeImportSummary summary2 = employeeImportService.importEmployees(file2);

        assertEquals(1, summary2.getTotalRecords());
        assertEquals(0, summary2.getCreatedCount());
        assertEquals(0, summary2.getUpdatedCount());
        assertEquals(1, summary2.getSkippedCount());
        assertEquals(0, summary2.getFailedCount());
        assertEquals(1, employeeRepository.findAll().size());
    }

    @Test
    void testImportModifiedData_Updated() throws IOException {
        String[][] initialData = {
                {"Member ID", "Name", "UAN", "IP Number", "Bank Account", "IFSC", "Category"},
                {"101", "John Doe", "100000000001", "2000000001", "30000000001", "SBIN0001234", "CL"}
        };
        employeeImportService.importEmployees(createExcelFile(initialData));

        String[][] updatedData = {
                {"Member ID", "Name", "UAN", "IP Number", "Bank Account", "IFSC", "Category"},
                {"101", "Johnathan Doe", "100000000001", "2000000001", "99999999999", "SBIN0009999", "HL"}
        };

        MockMultipartFile file = createExcelFile(updatedData);
        EmployeeImportSummary summary = employeeImportService.importEmployees(file);

        assertEquals(1, summary.getTotalRecords());
        assertEquals(0, summary.getCreatedCount());
        assertEquals(1, summary.getUpdatedCount());
        assertEquals(0, summary.getSkippedCount());
        assertEquals(0, summary.getFailedCount());

        Optional<Employee> updatedEmp = employeeRepository.findByMemberId("101");
        assertTrue(updatedEmp.isPresent());
        assertEquals("Johnathan Doe", updatedEmp.get().getFullName());
        assertEquals("99999999999", updatedEmp.get().getBankAccountNo());
        assertEquals(Employee.Category.HL, updatedEmp.get().getCategory());
    }

    @Test
    void testImportBlankMandatoryField_Failed() throws IOException {
        String[][] data = {
                {"Member ID", "Name", "UAN", "IP Number"},
                {"101", "", "100000000001", "2000000001"}, // Blank Name
                {"", "No Code User", "100000000002", "2000000002"} // Blank Member ID
        };

        MockMultipartFile file = createExcelFile(data);
        EmployeeImportSummary summary = employeeImportService.importEmployees(file);

        assertEquals(2, summary.getTotalRecords());
        assertEquals(0, summary.getCreatedCount());
        assertEquals(2, summary.getFailedCount());
        assertEquals(2, summary.getFailedRecords().size());
        assertTrue(summary.getFailedRecords().get(0).getReason().contains("Missing mandatory field: Employee Name"));
        assertTrue(summary.getFailedRecords().get(1).getReason().contains("Missing mandatory field: Employee Code"));
    }

    @Test
    void testImportConflict_Failed() throws IOException {
        Employee e1 = new Employee();
        e1.setMemberId("101");
        e1.setFullName("User One");
        e1.setUanNumber("111111111111");
        employeeRepository.save(e1);

        Employee e2 = new Employee();
        e2.setMemberId("102");
        e2.setFullName("User Two");
        e2.setUanNumber("222222222222");
        employeeRepository.save(e2);

        String[][] conflictingData = {
                {"Member ID", "Name", "UAN"},
                {"101", "User One Modified", "222222222222"}
        };

        MockMultipartFile file = createExcelFile(conflictingData);
        EmployeeImportSummary summary = employeeImportService.importEmployees(file);

        assertEquals(1, summary.getTotalRecords());
        assertEquals(0, summary.getCreatedCount());
        assertEquals(1, summary.getFailedCount());
        assertTrue(summary.getFailedRecords().get(0).getReason().contains("Conflict"));
    }

    // In-memory stub for EmployeeRepository avoiding ByteBuddy agent requirements
    private static class StubEmployeeRepository implements EmployeeRepository {
        private final Map<UUID, Employee> store = new HashMap<>();

        @Override
        public <S extends Employee> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Employee> findByMemberId(String memberId) {
            if (memberId == null) return Optional.empty();
            return store.values().stream()
                    .filter(e -> memberId.equalsIgnoreCase(e.getMemberId()))
                    .findFirst();
        }

        @Override
        public Optional<Employee> findByUanNumber(String uanNumber) {
            if (uanNumber == null) return Optional.empty();
            return store.values().stream()
                    .filter(e -> uanNumber.equalsIgnoreCase(e.getUanNumber()))
                    .findFirst();
        }

        @Override
        public Optional<Employee> findByIpNumber(String ipNumber) {
            if (ipNumber == null) return Optional.empty();
            return store.values().stream()
                    .filter(e -> ipNumber.equalsIgnoreCase(e.getIpNumber()))
                    .findFirst();
        }

        @Override
        public Optional<Employee> findByBankAccountNo(String bankAccountNo) {
            if (bankAccountNo == null) return Optional.empty();
            return store.values().stream()
                    .filter(e -> bankAccountNo.equalsIgnoreCase(e.getBankAccountNo()))
                    .findFirst();
        }

        @Override
        public Optional<Employee> findByAadhaarNumber(String aadhaarNumber) {
            if (aadhaarNumber == null) return Optional.empty();
            return store.values().stream()
                    .filter(e -> aadhaarNumber.equalsIgnoreCase(e.getAadhaarNumber()))
                    .findFirst();
        }

        @Override
        public Optional<Employee> findByPanNumber(String panNumber) {
            if (panNumber == null) return Optional.empty();
            return store.values().stream()
                    .filter(e -> panNumber.equalsIgnoreCase(e.getPanNumber()))
                    .findFirst();
        }

        @Override
        public List<Employee> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<Employee> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override public void deleteAll() { store.clear(); }

        // Unused JpaRepository methods for testing
        @Override public void flush() {}
        @Override public <S extends Employee> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Employee> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<Employee> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Employee getOne(UUID id) { return store.get(id); }
        @Override public Employee getById(UUID id) { return store.get(id); }
        @Override public Employee getReferenceById(UUID id) { return store.get(id); }
        @Override public <S extends Employee> List<S> findAll(Example<S> example) { return null; }
        @Override public <S extends Employee> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override public <S extends Employee> List<S> saveAll(Iterable<S> entities) {
            List<S> res = new ArrayList<>();
            for (S e : entities) res.add(save(e));
            return res;
        }
        @Override public boolean existsById(UUID id) { return store.containsKey(id); }
        @Override public List<Employee> findAllById(Iterable<UUID> uuids) { return null; }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(UUID id) { store.remove(id); }
        @Override public void delete(Employee entity) { if (entity != null) store.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends UUID> uuids) {}
        @Override public void deleteAll(Iterable<? extends Employee> entities) {}
        @Override public List<Employee> findAll(Sort sort) { return findAll(); }
        @Override public Page<Employee> findAll(Pageable pageable) { return null; }
        @Override public <S extends Employee> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Employee> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Employee> long count(Example<S> example) { return 0; }
        @Override public <S extends Employee> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Employee, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }
}
