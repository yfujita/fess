<%@page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<html>
<head profile="http://a9.com/-/spec/opensearch/1.1/">
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="x-ua-compatible" content="ie=edge">
<title><la:message key="labels.search_title" /></title>
<c:if test="${osddLink}">
	<link rel="search" type="application/opensearchdescription+xml"
		href="${f:url('/osdd')}"
		title="<la:message key="labels.index_osdd_title" />" />
</c:if>
<link href="${f:url('/css/style-base.css')}" rel="stylesheet"
	type="text/css" />
<link href="${f:url('/css/style.css')}" rel="stylesheet" type="text/css" />
<link href="${f:url('/css/admin/font-awesome.min.css')}"
	rel="stylesheet" type="text/css" />
</head>
<body>
	<nav class="navbar navbar-dark bg-inverse navbar-static-top pos-f-t">
		<div class="container">
			<ul class="nav navbar-nav pull-right">
				<c:if test="${!empty username && username != 'guest'}">
					<li class="nav-item">
						<div class="dropdown">
							<a class="nav-link dropdown-toggle" data-toggle="dropdown"
								href="#" role="button" aria-haspopup="true"
								aria-expanded="false"> <i class="fa fa-user"></i>${username}
							</a>
							<div class="dropdown-menu" aria-labelledby="userMenu">
								<la:link href="/logout" styleClass="dropdown-item">
									<la:message key="labels.logout" />
								</la:link>
							</div>
						</div>
					</li>
				</c:if>
				<li class="nav-item"><la:link href="/help"
						styleClass="nav-link help-link">
						<i class="fa fa-question-circle"></i>
						<la:message key="labels.index_help" />
					</la:link></li>
			</ul>
		</div>
	</nav>
	<div class="container">
		<div class="row content">
			<div class="center-block searchFormBox">
				<h1 class="mainLogo">
					<img src="${f:url('/images/logo.png')}"
						alt="<la:message key="labels.index_title" />" />
				</h1>
				<div>
					<la:info id="msg" message="true">
						<div class="alert-message info">${msg}</div>
					</la:info>
					<la:errors header="errors.front_header"
						footer="errors.front_footer" prefix="errors.front_prefix"
						suffix="errors.front_suffix" />
				</div>
				<la:form styleClass="form-stacked" action="search" method="get"
					styleId="searchForm">
					${fe:facetForm()}${fe:geoForm()}
					<fieldset>
						<div class="clearfix">
							<div class="input">
								<la:text styleClass="query form-control center-block"
									property="q" size="50" maxlength="1000"
									styleId="contentQuery" autocomplete="off" />
							</div>
						</div>
						<c:if test="${fe:hswsize(null) != 0}">
							<div>
								<p class="hotSearchWordBody ellipsis">
									<la:message key="labels.search_hot_search_word" />
									<c:forEach var="item" items="${fe:hsw(null, 5)}">
										<la:link
											href="/search/search?q=${f:u(item)}${fe:facetQuery()}${fe:geoQuery()}">${f:h(item)}</la:link>
									</c:forEach>
								</p>
							</div>
						</c:if>
						<div class="clearfix searchButtonBox btn-group">
							<button type="submit" name="search" id="searchButton"
								class="btn btn-primary">
								<i class="fa fa-search"></i>
								<la:message key="labels.index_form_search_btn" />
							</button>
							<button type="button" class="btn btn-secondary"
								data-toggle="modal" data-target="#searchOptions">
								<i class="fa fa-cog"></i>
								<la:message key="labels.index_form_option_btn" />
							</button>
						</div>
					</fieldset>
					<div class="modal fade" id="searchOptions" tabindex="-1"
						role="dialog" aria-labelledby="searchOptionsLabel"
						aria-hidden="true">
						<div class="modal-dialog" role="document">
							<div class="modal-content">
								<div class="modal-header">
									<button type="button" class="close" data-dismiss="modal"
										aria-label="Close">
										<span aria-hidden="true">&times;</span> <span class="sr-only">Close</span>
									</button>
									<h4 class="modal-title" id="searchOptionsLabel">
										<la:message key="labels.search_options" />
									</h4>
								</div>
								<div class="modal-body">
									<fieldset class="form-group">
										<label for="contentNum"><la:message
												key="labels.index_num" /></label>
										<la:select property="num" styleId="numSearchOption"
											styleClass="form-control">
											<option value="">
												<la:message key="labels.search_result_select_num" />
											</option>
											<la:option value="10">10</la:option>
											<la:option value="20">20</la:option>
											<la:option value="30">30</la:option>
											<la:option value="40">40</la:option>
											<la:option value="50">50</la:option>
											<la:option value="100">100</la:option>
										</la:select>
									</fieldset>
									<fieldset class="form-group">
										<label for="contentSort"><la:message
												key="labels.index_sort" /></label>
										<la:select property="sort" styleId="sortSearchOption"
											styleClass="form-control">
											<option value="">
												<la:message key="labels.search_result_select_sort" />
											</option>
											<la:option value="created.asc">
												<la:message key="labels.search_result_sort_created_asc" />
											</la:option>
											<la:option value="created.desc">
												<la:message key="labels.search_result_sort_created_desc" />
											</la:option>
											<la:option value="contentLength.asc">
												<la:message
													key="labels.search_result_sort_contentLength_asc" />
											</la:option>
											<la:option value="contentLength.desc">
												<la:message
													key="labels.search_result_sort_contentLength_desc" />
											</la:option>
											<la:option value="lastModified.asc">
												<la:message key="labels.search_result_sort_lastModified_asc" />
											</la:option>
											<la:option value="lastModified.desc">
												<la:message
													key="labels.search_result_sort_lastModified_desc" />
											</la:option>
											<c:if test="${searchLogSupport}">
												<la:option value="clickCount_l_x_dv.asc">
													<la:message key="labels.search_result_sort_clickCount_asc" />
												</la:option>
												<la:option value="clickCount_l_x_dv.desc">
													<la:message key="labels.search_result_sort_clickCount_desc" />
												</la:option>
											</c:if>
											<c:if test="${favoriteSupport}">
												<la:option value="favoriteCount_l_x_dv.asc">
													<la:message
														key="labels.search_result_sort_favoriteCount_asc" />
												</la:option>
												<la:option value="favoriteCount_l_x_dv.desc">
													<la:message
														key="labels.search_result_sort_favoriteCount_desc" />
												</la:option>
											</c:if>
										</la:select>
									</fieldset>
									<fieldset class="form-group">
										<label for="contentLang"><la:message
												key="labels.index_lang" /></label>
										<la:select property="lang" styleId="langSearchOption"
											multiple="true" styleClass="form-control">
											<c:forEach var="item" items="${langItems}">
												<la:option value="${f:u(item.value)}">
																	${f:h(item.label)}
																</la:option>
											</c:forEach>
										</la:select>
									</fieldset>
									<c:if test="${displayLabelTypeItems}">
										<fieldset class="form-group">
											<label for="contentLabelType"><la:message
													key="labels.index_label" /></label>
											<la:select property="fields.label"
												styleId="labelTypeSearchOption" multiple="true"
												styleClass="form-control">
												<c:forEach var="item" items="${labelTypeItems}">
													<la:option value="${f:u(item.value)}">
														${f:h(item.label)}
													</la:option>
												</c:forEach>
											</la:select>
										</fieldset>
									</c:if>
								</div>
								<div class="modal-footer">
									<button class="btn btn-secondary" id="searchOptionsClearButton">
										<la:message key="labels.search_options_clear" />
									</button>
									<button class="btn btn-secondary" data-dismiss="modal">
										<la:message key="labels.search_options_close" />
									</button>
								</div>
							</div>
						</div>
					</div>
				</la:form>
			</div>
		</div>
		<jsp:include page="footer.jsp" />
	</div>
	<input type="hidden" id="contextPath"
		value="<%=request.getContextPath()%>" />
	<script type="text/javascript"
		src="${f:url('/js/jquery-2.1.4.min.js')}"></script>
	<script type="text/javascript" src="${f:url('/js/bootstrap.js')}"></script>
	<script type="text/javascript" src="${f:url('/js/suggestor.js')}"></script>
	<script type="text/javascript" src="${f:url('/js/index.js')}"></script>
</body>
</html>
