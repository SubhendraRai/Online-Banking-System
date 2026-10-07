<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="pageTitle" value="Transaction History | Apex Banking System" scope="request"/>
<jsp:include page="/WEB-INF/views/layout/header.jsp"/>

<div class="mb-3" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem;">
    <div>
        <h1 style="font-size:1.85rem; font-weight:800; letter-spacing:-0.02em;">
            Transaction History & Statement
        </h1>
        <p style="color:var(--color-text-muted);">
            View, search, and filter posted ledger transactions across your accounts.
        </p>
    </div>
    <div style="display:flex; gap:0.5rem;">
        <a href="<c:url value='/customer/transfer'/>" class="btn btn-primary btn-sm">&#10148; New Transfer</a>
    </div>
</div>

<!-- Dynamic Search & Filter Form -->
<div class="card mb-3">
    <div class="card-body">
        <form action="<c:url value='/customer/transactions'/>" method="get" id="filterForm">
            <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap:1rem; align-items:end;">
                <div class="form-group" style="margin-bottom:0;">
                    <label for="accountNo" class="form-label">Account</label>
                    <select name="accountNo" id="accountNo" class="form-control">
                        <c:forEach var="acc" items="${accounts}">
                            <option value="<c:out value='${acc.accountNo}'/>"
                                ${acc.accountNo == selectedAccountNo ? 'selected' : ''}>
                                <c:out value="${acc.accountNo}"/> (<c:out value="${acc.accountType}"/>)
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group" style="margin-bottom:0;">
                    <label for="type" class="form-label">Transaction Type</label>
                    <select name="type" id="type" class="form-control">
                        <option value="">All Types</option>
                        <option value="DEPOSIT" ${filter.type == 'DEPOSIT' ? 'selected' : ''}>Deposit</option>
                        <option value="WITHDRAWAL" ${filter.type == 'WITHDRAWAL' ? 'selected' : ''}>Withdrawal</option>
                        <option value="TRANSFER" ${filter.type == 'TRANSFER' ? 'selected' : ''}>Transfer</option>
                    </select>
                </div>

                <div class="form-group" style="margin-bottom:0;">
                    <label for="fromDate" class="form-label">From Date</label>
                    <input type="date" name="fromDate" id="fromDate" class="form-control"
                           value="<c:out value='${filter.fromDate}'/>">
                </div>

                <div class="form-group" style="margin-bottom:0;">
                    <label for="toDate" class="form-label">To Date</label>
                    <input type="date" name="toDate" id="toDate" class="form-control"
                           value="<c:out value='${filter.toDate}'/>">
                </div>

                <div class="form-group" style="margin-bottom:0;">
                    <label for="minAmount" class="form-label">Min Amount (₹)</label>
                    <input type="number" name="minAmount" id="minAmount" class="form-control"
                           placeholder="0.00" min="0" step="0.01" value="<c:out value='${filter.minAmount}'/>">
                </div>

                <div class="form-group" style="margin-bottom:0;">
                    <label for="maxAmount" class="form-label">Max Amount (₹)</label>
                    <input type="number" name="maxAmount" id="maxAmount" class="form-control"
                           placeholder="0.00" min="0" step="0.01" value="<c:out value='${filter.maxAmount}'/>">
                </div>

                <div class="form-group" style="margin-bottom:0; grid-column: span 2;">
                    <label for="keyword" class="form-label">Keyword / Search</label>
                    <input type="text" name="keyword" id="keyword" class="form-control"
                           placeholder="Search remarks, memo, or account number..."
                           value="<c:out value='${filter.keyword}'/>">
                </div>

                <div style="display:flex; gap:0.5rem;">
                    <button type="submit" class="btn btn-primary" style="flex:1;">
                        Filter
                    </button>
                    <a href="<c:url value='/customer/transactions?accountNo=${selectedAccountNo}'/>" class="btn btn-secondary">
                        Reset
                    </a>
                </div>
            </div>
        </form>
    </div>
</div>

<!-- Ledger Transactions Table -->
<div class="card mb-3">
    <div class="card-header">
        <div>
            <h2 class="card-title">Statement Records (Account: <c:out value="${selectedAccountNo}"/>)</h2>
            <p class="card-subtitle">
                Showing
                <c:choose>
                    <c:when test="${pagedTxns.totalItems > 0}">
                        <c:out value="${(pagedTxns.page - 1) * pagedTxns.size + 1}"/> -
                        <c:out value="${pagedTxns.page * pagedTxns.size > pagedTxns.totalItems ? pagedTxns.totalItems : pagedTxns.page * pagedTxns.size}"/>
                        of <c:out value="${pagedTxns.totalItems}"/> transactions
                    </c:when>
                    <c:otherwise>0 transactions</c:otherwise>
                </c:choose>
            </p>
        </div>
    </div>
    <div class="card-body" style="padding:0;">
        <div class="table-responsive">
            <table class="table">
                <thead>
                    <tr>
                        <th>Txn ID</th>
                        <th>Timestamp</th>
                        <th>Type</th>
                        <th>Details / Remarks</th>
                        <th class="text-right">Amount</th>
                        <th class="text-center">Status</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${not empty pagedTxns and not empty pagedTxns.items}">
                            <c:forEach var="txn" items="${pagedTxns.items}">
                                <c:set var="isCredit" value="${txn.toAccount == selectedAccountNo}"/>
                                <tr>
                                    <td style="font-family:monospace; font-weight:700; color:var(--color-text-muted);">
                                        #<c:out value="${txn.txnId}"/>
                                    </td>
                                    <td><c:out value="${txn.createdAt}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${txn.txnType == 'DEPOSIT'}">
                                                <span class="badge badge-active">DEPOSIT</span>
                                            </c:when>
                                            <c:when test="${txn.txnType == 'WITHDRAWAL'}">
                                                <span class="badge badge-pending">WITHDRAW</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-info">TRANSFER</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <div style="font-weight:600;"><c:out value="${txn.remarks}"/></div>
                                        <div style="font-size:0.8rem; color:var(--color-text-muted);">
                                            <c:choose>
                                                <c:when test="${txn.txnType == 'TRANSFER'}">
                                                    <c:choose>
                                                        <c:when test="${isCredit}">
                                                            From: <c:out value="${txn.fromAccount}"/>
                                                        </c:when>
                                                        <c:otherwise>
                                                            To: <c:out value="${txn.toAccount}"/>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:when>
                                                <c:when test="${txn.txnType == 'DEPOSIT'}">
                                                    Self-credited
                                                </c:when>
                                                <c:otherwise>
                                                    Cash / Debit
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </td>
                                    <td class="text-right" style="font-weight:700; font-size:1.05rem; color:${isCredit ? 'var(--color-success)' : 'var(--color-danger)'};">
                                        <c:choose>
                                            <c:when test="${isCredit}">+</c:when>
                                            <c:otherwise>-</c:otherwise>
                                        </c:choose><c:out value="${txn.formattedAmount}"/>
                                    </td>
                                    <td class="text-center">
                                        <span class="badge badge-active"><c:out value="${txn.status}"/></span>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="6" class="text-center" style="padding:2.5rem; color:var(--color-text-muted);">
                                    No transactions match the specified filter criteria.
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Pagination Controls -->
    <c:if test="${pagedTxns.totalPages > 1}">
        <c:url var="baseUrl" value="/customer/transactions">
            <c:param name="accountNo" value="${selectedAccountNo}"/>
            <c:if test="${not empty filter.type}"><c:param name="type" value="${filter.type}"/></c:if>
            <c:if test="${not empty filter.fromDate}"><c:param name="fromDate" value="${filter.fromDate}"/></c:if>
            <c:if test="${not empty filter.toDate}"><c:param name="toDate" value="${filter.toDate}"/></c:if>
            <c:if test="${not empty filter.minAmount}"><c:param name="minAmount" value="${filter.minAmount}"/></c:if>
            <c:if test="${not empty filter.maxAmount}"><c:param name="maxAmount" value="${filter.maxAmount}"/></c:if>
            <c:if test="${not empty filter.keyword}"><c:param name="keyword" value="${filter.keyword}"/></c:if>
        </c:url>

        <div class="card-footer" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem;">
            <div style="font-size:0.875rem; color:var(--color-text-muted);">
                Page <strong><c:out value="${pagedTxns.page}"/></strong> of <strong><c:out value="${pagedTxns.totalPages}"/></strong>
            </div>

            <div style="display:flex; gap:0.25rem;">
                <c:choose>
                    <c:when test="${pagedTxns.hasPrevious()}">
                        <a href="<c:out value='${baseUrl}&page=1'/>" class="btn btn-secondary btn-sm">&laquo; First</a>
                        <a href="<c:out value='${baseUrl}&page=${pagedTxns.page - 1}'/>" class="btn btn-secondary btn-sm">&lsaquo; Prev</a>
                    </c:when>
                    <c:otherwise>
                        <button class="btn btn-secondary btn-sm" disabled style="opacity:0.5;">&laquo; First</button>
                        <button class="btn btn-secondary btn-sm" disabled style="opacity:0.5;">&lsaquo; Prev</button>
                    </c:otherwise>
                </c:choose>

                <c:forEach var="pageNum" begin="${pagedTxns.page > 2 ? pagedTxns.page - 2 : 1}"
                           end="${pagedTxns.page + 2 < pagedTxns.totalPages ? pagedTxns.page + 2 : pagedTxns.totalPages}">
                    <c:choose>
                        <c:when test="${pageNum == pagedTxns.page}">
                            <span class="btn btn-primary btn-sm" style="font-weight:700;"><c:out value="${pageNum}"/></span>
                        </c:when>
                        <c:otherwise>
                            <a href="<c:out value='${baseUrl}&page=${pageNum}'/>" class="btn btn-secondary btn-sm"><c:out value="${pageNum}"/></a>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>

                <c:choose>
                    <c:when test="${pagedTxns.hasNext()}">
                        <a href="<c:out value='${baseUrl}&page=${pagedTxns.page + 1}'/>" class="btn btn-secondary btn-sm">Next &rsaquo;</a>
                        <a href="<c:out value='${baseUrl}&page=${pagedTxns.totalPages}'/>" class="btn btn-secondary btn-sm">Last &raquo;</a>
                    </c:when>
                    <c:otherwise>
                        <button class="btn btn-secondary btn-sm" disabled style="opacity:0.5;">Next &rsaquo;</button>
                        <button class="btn btn-secondary btn-sm" disabled style="opacity:0.5;">Last &raquo;</button>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </c:if>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp"/>
