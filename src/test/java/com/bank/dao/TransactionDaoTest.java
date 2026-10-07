package com.bank.dao;

import com.bank.model.Transaction;
import com.bank.model.TxnStatus;
import com.bank.model.TxnType;
import com.bank.util.Money;
import com.bank.util.Page;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDaoTest extends BaseDaoIntegrationTest {

    private TransactionDao transactionDao;

    @BeforeEach
    void setUp() {
        transactionDao = new TransactionDao();
    }

    @Test
    @DisplayName("insert records a ledger entry and populates generated txnId")
    void testInsertTransaction() {
        Transaction txn = Transaction.transfer("100000000001", "100000000003",
                new BigDecimal("2500.00"), "Monthly allowance");
        Transaction saved = transactionDao.insert(txn);

        assertNotNull(saved.getTxnId());
        Optional<Transaction> fetched = transactionDao.findById(saved.getTxnId());
        assertTrue(fetched.isPresent());
        assertEquals("100000000001", fetched.get().getFromAccount());
        assertEquals("100000000003", fetched.get().getToAccount());
        assertEquals(0, new BigDecimal("2500.00").compareTo(fetched.get().getAmount()));
        assertEquals(TxnStatus.SUCCESS, fetched.get().getStatus());
    }

    @Test
    @DisplayName("findByAccount with TxnFilter filters by keyword matching remarks and counterparty account")
    void testFindByAccountKeywordFiltering() {
        // Seed 3 transactions
        Transaction t1 = Transaction.transfer("100000000001", "100000000003",
                new BigDecimal("500.00"), "Lunch reimbursement");
        Transaction t2 = Transaction.transfer("100000000002", "100000000001",
                new BigDecimal("1200.00"), "Rent contribution");
        Transaction t3 = Transaction.deposit("100000000001", new BigDecimal("3000.00"), "Salary bonus");
        transactionDao.insert(t1);
        transactionDao.insert(t2);
        transactionDao.insert(t3);

        // Filter 1: Keyword matching remarks ("Lunch")
        TxnFilter remarkFilter = TxnFilter.builder().keyword("Lunch").build();
        Page<Transaction> page1 = transactionDao.findByAccount("100000000001", remarkFilter, 1, 10);
        assertEquals(1, page1.getTotalItems());
        assertEquals("Lunch reimbursement", page1.getItems().get(0).getRemarks());

        // Filter 2: Keyword matching counterparty account number ("100000000002")
        TxnFilter counterpartyFilter = TxnFilter.builder().keyword("100000000002").build();
        Page<Transaction> page2 = transactionDao.findByAccount("100000000001", counterpartyFilter, 1, 10);
        assertEquals(1, page2.getTotalItems());
        assertEquals("Rent contribution", page2.getItems().get(0).getRemarks());

        // Filter 3: Amount range
        TxnFilter amountFilter = TxnFilter.builder()
                .minAmount(new BigDecimal("1000.00"))
                .maxAmount(new BigDecimal("2000.00"))
                .build();
        Page<Transaction> page3 = transactionDao.findByAccount("100000000001", amountFilter, 1, 10);
        assertEquals(1, page3.getTotalItems());
        assertEquals(0, new BigDecimal("1200.00").compareTo(page3.getItems().get(0).getAmount()));
    }

    @Test
    @DisplayName("findByAccount correctly calculates pagination slices")
    void testFindByAccountPagination() {
        for (int i = 1; i <= 5; i++) {
            transactionDao.insert(Transaction.deposit("100000000001",
                    new BigDecimal("100.00"), "Deposit #" + i));
        }

        Page<Transaction> pageSlice = transactionDao.findByAccount("100000000001", null, 1, 2);
        assertEquals(5, pageSlice.getTotalItems());
        assertEquals(2, pageSlice.getItems().size());
        assertEquals(3, pageSlice.getTotalPages());
        assertTrue(pageSlice.hasNext());
        assertFalse(pageSlice.hasPrevious());
    }

    @Test
    @DisplayName("sumTransfersToday aggregates only successful transfers debited today")
    void testSumTransfersToday() {
        String fromAcc = "100000000001";
        String toAcc = "100000000003";

        // Transfer 1: Today, Success, 1500.00
        transactionDao.insert(Transaction.transfer(fromAcc, toAcc, new BigDecimal("1500.00"), "Txn 1"));
        // Transfer 2: Today, Success, 2500.00
        transactionDao.insert(Transaction.transfer(fromAcc, toAcc, new BigDecimal("2500.00"), "Txn 2"));
        // Deposit (not a transfer): Should NOT be counted
        transactionDao.insert(Transaction.deposit(fromAcc, new BigDecimal("5000.00"), "Self deposit"));

        BigDecimal sumToday = transactionDao.sumTransfersToday(fromAcc);
        assertEquals(0, new BigDecimal("4000.00").compareTo(sumToday));
    }
}
