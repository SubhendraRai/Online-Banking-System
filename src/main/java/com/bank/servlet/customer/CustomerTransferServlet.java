package com.bank.servlet.customer;

import com.bank.exception.BankingException;
import com.bank.model.Account;
import com.bank.model.SessionUser;
import com.bank.model.Transaction;
import com.bank.model.TransferDraft;
import com.bank.model.TransferReceipt;
import com.bank.service.AccountService;
import com.bank.service.TransferService;
import com.bank.util.ErrorMessageResolver;
import com.bank.util.FlashMessage;
import com.bank.util.Money;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customer servlet coordinating the 3-step fund transfer workflow:
 * <ol>
 *   <li><b>Step 1 (Form):</b> Select source account, enter recipient account and transfer details.</li>
 *   <li><b>Step 2 (Confirmation):</b> Verify verified recipient name and amount with a one-time session token.</li>
 *   <li><b>Step 3 (Execute & Receipt):</b> Execute transfer, enforce idempotency/double-submission defense,
 *       and render receipt with transaction ID via Post/Redirect/Get.</li>
 * </ol>
 */
@WebServlet(name = "CustomerTransferServlet", urlPatterns = {"/customer/transfer"})
public class CustomerTransferServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerTransferServlet.class);

    private final AccountService accountService;
    private final TransferService transferService;

    public CustomerTransferServlet() {
        this(new AccountService(), new TransferService());
    }

    public CustomerTransferServlet(AccountService accountService, TransferService transferService) {
        this.accountService = Objects.requireNonNull(accountService, "accountService cannot be null");
        this.transferService = Objects.requireNonNull(transferService, "transferService cannot be null");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof SessionUser user)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String step = request.getParameter("step");
        if ("confirm".equalsIgnoreCase(step)) {
            showConfirmationPage(request, response, session);
        } else if ("receipt".equalsIgnoreCase(step)) {
            showReceiptPage(request, response, session);
        } else if ("cancel".equalsIgnoreCase(step)) {
            session.removeAttribute(TransferDraft.SESSION_KEY);
            FlashMessage.info(session, "Fund transfer cancelled.");
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
        } else {
            showTransferFormPage(request, response, user);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof SessionUser user)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if ("execute".equalsIgnoreCase(action)) {
            executeTransfer(request, response, session, user);
        } else {
            reviewAndStageTransfer(request, response, session, user);
        }
    }

    private void showTransferFormPage(HttpServletRequest request, HttpServletResponse response,
                                      SessionUser user) throws ServletException, IOException {
        List<Account> accounts = accountService.getAccountsForUser(user.getId());
        request.setAttribute("accounts", accounts);
        request.getRequestDispatcher("/WEB-INF/views/customer/transfer.jsp").forward(request, response);
    }

    private void showConfirmationPage(HttpServletRequest request, HttpServletResponse response,
                                      HttpSession session) throws ServletException, IOException {
        TransferDraft draft = (TransferDraft) session.getAttribute(TransferDraft.SESSION_KEY);
        if (draft == null) {
            FlashMessage.warning(session, "No pending transfer found or your session expired.");
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
            return;
        }

        request.setAttribute("draft", draft);
        request.getRequestDispatcher("/WEB-INF/views/customer/transfer-confirm.jsp").forward(request, response);
    }

    private void showReceiptPage(HttpServletRequest request, HttpServletResponse response,
                                 HttpSession session) throws ServletException, IOException {
        TransferReceipt receipt = (TransferReceipt) session.getAttribute(TransferReceipt.SESSION_KEY);
        if (receipt == null) {
            response.sendRedirect(request.getContextPath() + "/customer/dashboard");
            return;
        }

        request.setAttribute("receipt", receipt);
        request.getRequestDispatcher("/WEB-INF/views/customer/transfer-receipt.jsp").forward(request, response);
    }

    private void reviewAndStageTransfer(HttpServletRequest request, HttpServletResponse response,
                                        HttpSession session, SessionUser user) throws IOException {
        String fromAccount = request.getParameter("fromAccount");
        String toAccount = request.getParameter("toAccount");
        String amountStr = request.getParameter("amount");
        String remarks = request.getParameter("remarks");

        try {
            if (fromAccount == null || fromAccount.isBlank()) {
                throw new IllegalArgumentException("Please select a source account.");
            }
            if (toAccount == null || toAccount.isBlank()) {
                throw new IllegalArgumentException("Please enter a destination account number.");
            }
            if (fromAccount.trim().equals(toAccount.trim())) {
                throw new IllegalArgumentException("Source and destination accounts must be different.");
            }

            Account fromAcc = accountService.getUserAccount(fromAccount.trim(), user.getId());
            BigDecimal amount = parseTransferAmount(amountStr);

            if (amount.compareTo(fromAcc.getAvailableBalance()) > 0) {
                throw new IllegalArgumentException(String.format(
                        "Transfer amount %s exceeds available balance %s in account %s.",
                        Money.formatInr(amount), Money.formatInr(fromAcc.getAvailableBalance()), fromAccount));
            }

            String recipientName = transferService.getRecipientName(toAccount.trim());

            String formToken = UUID.randomUUID().toString();
            TransferDraft draft = new TransferDraft(
                    formToken, fromAccount.trim(), toAccount.trim(), recipientName, amount, remarks);

            session.setAttribute(TransferDraft.SESSION_KEY, draft);
            response.sendRedirect(request.getContextPath() + "/customer/transfer?step=confirm");

        } catch (BankingException be) {
            LOGGER.warn("Transfer staging failed for customer ID {}: {}", user.getId(), be.getMessage());
            FlashMessage.error(session, ErrorMessageResolver.resolve(be));
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Invalid transfer request from customer ID {}: {}", user.getId(), iae.getMessage());
            FlashMessage.error(session, iae.getMessage());
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
        } catch (Exception e) {
            LOGGER.error("Unexpected error staging transfer for customer ID {}", user.getId(), e);
            FlashMessage.error(session, ErrorMessageResolver.resolve(e));
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
        }
    }

    private void executeTransfer(HttpServletRequest request, HttpServletResponse response,
                                 HttpSession session, SessionUser user) throws IOException {
        String submittedToken = request.getParameter("token");
        TransferDraft draft = (TransferDraft) session.getAttribute(TransferDraft.SESSION_KEY);

        // One-time token verification to block duplicate submissions
        if (draft == null || submittedToken == null || !submittedToken.equals(draft.getToken())) {
            LOGGER.warn("Duplicate or invalid token transfer execution blocked for customer ID {}", user.getId());
            FlashMessage.error(session, "This transfer has already been submitted or the session expired.");
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
            return;
        }

        // Atomically consume token from session before executing
        session.removeAttribute(TransferDraft.SESSION_KEY);

        try {
            Transaction txn = transferService.transfer(
                    draft.getFromAccount(), draft.getToAccount(), draft.getAmount(), draft.getRemarks());

            LOGGER.info("Transfer completed successfully. Txn ID: {}, From: {}, To: {}, Amount: {}",
                    txn.getTxnId(), draft.getFromAccount(), draft.getToAccount(), Money.formatInr(draft.getAmount()));

            TransferReceipt receipt = new TransferReceipt(
                    txn.getTxnId(), draft.getFromAccount(), draft.getToAccount(),
                    draft.getRecipientName(), draft.getAmount(), draft.getRemarks(), txn.getCreatedAt());

            session.setAttribute(TransferReceipt.SESSION_KEY, receipt);
            FlashMessage.success(session, "Transfer of " + Money.formatInr(draft.getAmount())
                    + " completed successfully! Txn ID: #" + txn.getTxnId());

            response.sendRedirect(request.getContextPath() + "/customer/transfer?step=receipt");

        } catch (BankingException be) {
            LOGGER.warn("Transfer execution failed for customer ID {}: {}", user.getId(), be.getMessage());
            FlashMessage.error(session, ErrorMessageResolver.resolve(be));
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
        } catch (Exception e) {
            LOGGER.error("Unexpected error executing transfer for customer ID {}", user.getId(), e);
            FlashMessage.error(session, ErrorMessageResolver.resolve(e));
            response.sendRedirect(request.getContextPath() + "/customer/transfer");
        }
    }

    private BigDecimal parseTransferAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Transfer amount cannot be empty.");
        }
        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Transfer amount must be strictly greater than 0.00.");
            }
            if (amount.scale() > 2) {
                throw new IllegalArgumentException("Transfer amount cannot exceed 2 decimal places.");
            }
            return amount;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Please enter a valid numeric transfer amount (e.g., 1000.00).");
        }
    }
}
