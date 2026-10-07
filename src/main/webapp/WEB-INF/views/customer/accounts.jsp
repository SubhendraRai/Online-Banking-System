<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="My Accounts | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="mb-3">
    <h1 style="font-size:1.85rem; font-weight:800; letter-spacing:-0.02em;">
        Account Portfolio Management
    </h1>
    <p style="color:var(--color-text-muted);">
        Review your current depository accounts or open an additional savings or commercial current account.
    </p>
</div>

<div class="grid grid-cols-3 mb-3" style="align-items:start;">
    <!-- Accounts List (2 Columns wide) -->
    <div style="grid-column: span 2;">
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">Active Depository Accounts</h2>
                    <p class="card-subtitle">Accounts currently in operational standing</p>
                </div>
            </div>
            <div class="card-body" style="padding:0;">
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Account No</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th class="text-right">Ledger Balance</th>
                                <th class="text-right">Available Balance</th>
                                <th class="text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty accounts}">
                                    <c:forEach var="acc" items="${accounts}">
                                        <tr>
                                            <td style="font-weight:700; font-family:monospace; font-size:1rem;">
                                                <c:out value="${acc.accountNo}"/>
                                            </td>
                                            <td>
                                                <span class="badge badge-info"><c:out value="${acc.accountType}"/></span>
                                            </td>
                                            <td>
                                                <span class="badge badge-active"><c:out value="${acc.status}"/></span>
                                            </td>
                                            <td class="text-right" style="font-weight:600;">
                                                <c:out value="${acc.formattedBalance}"/>
                                            </td>
                                            <td class="text-right" style="font-weight:700; color:var(--color-primary-dark);">
                                                <c:out value="${acc.formattedAvailableBalance}"/>
                                            </td>
                                            <td class="text-center">
                                                <a href="<c:url value='/customer/transactions?accountNo=${acc.accountNo}'/>"
                                                   class="btn btn-secondary btn-sm" style="padding:0.25rem 0.6rem; font-size:0.8rem;">
                                                    Statement
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="6" class="text-center" style="padding:2rem; color:var(--color-text-muted);">
                                            No accounts opened yet. Use the form on the right to open your first account.
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <!-- Open Another Account Form (1 Column wide) -->
    <div>
        <div class="card">
            <div class="card-header">
                <h2 class="card-title">Open New Account</h2>
                <p class="card-subtitle">Choose account type and starting balance</p>
            </div>
            <div class="card-body">
                <form action="<c:url value='/customer/accounts'/>" method="post" id="openAccountForm">
                    <div class="form-group">
                        <label for="accountType" class="form-label">Account Type</label>
                        <select name="accountType" id="accountType" class="form-control" required onchange="handleTypeChange()">
                            <option value="SAVINGS">Savings Account (Min ₹500.00)</option>
                            <option value="CURRENT">Current Account (Overdraft enabled)</option>
                        </select>
                        <span class="form-hint" id="typeHint">
                            Savings requires a minimum balance of ₹500.00.
                        </span>
                    </div>

                    <div class="form-group">
                        <label for="initialDeposit" class="form-label">Initial Opening Deposit (INR)</label>
                        <input type="number" name="initialDeposit" id="initialDeposit" class="form-control"
                               placeholder="e.g. 1000.00" min="0" step="0.01" value="500.00" required>
                        <span class="form-hint">
                            Must be ₹0.00 or at least ₹500.00 for Savings.
                        </span>
                    </div>

                    <button type="submit" class="btn btn-primary" style="width:100%;">
                        + Open Account Now
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
function handleTypeChange() {
    const typeSelect = document.getElementById('accountType');
    const depositInput = document.getElementById('initialDeposit');
    const hint = document.getElementById('typeHint');
    if (typeSelect.value === 'SAVINGS') {
        hint.textContent = 'Savings requires a minimum balance of ₹500.00.';
        depositInput.min = '500.00';
        if (parseFloat(depositInput.value || 0) < 500) {
            depositInput.value = '500.00';
        }
    } else {
        hint.textContent = 'Current accounts provide commercial overdraft facility.';
        depositInput.min = '0.00';
    }
}
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
