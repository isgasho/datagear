/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datagear.management.domain.Authorization;
import org.datagear.management.domain.Schema;
import org.datagear.management.domain.User;
import org.datagear.management.service.AuthorizationService;
import org.datagear.management.service.impl.AuthorizationQueryContext;
import org.datagear.persistence.PagingQuery;
import org.datagear.util.IDUtil;
import org.datagear.web.OperationMessage;
import org.datagear.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 授权管理控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/authorization")
public class AuthorizationController extends AbstractController
{
	/**
	 * 指定授权资源参数，设置后，所有CRUD操作都只针对这一个资源。
	 */
	public static final String PARAM_APPOINT_RESOURCE = "appointResource";

	@Autowired
	private AuthorizationService authorizationService;

	public AuthorizationController()
	{
		super();
	}

	public AuthorizationController(AuthorizationService authorizationService)
	{
		super();
		this.authorizationService = authorizationService;
	}

	public AuthorizationService getAuthorizationService()
	{
		return authorizationService;
	}

	public void setAuthorizationService(AuthorizationService authorizationService)
	{
		this.authorizationService = authorizationService;
	}

	@RequestMapping("/add")
	public String add(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
	{
		User user = WebUtils.getUser(request, response);

		setAppoiontResourceAttributeIf(request, model);
		model.addAttribute("user", user);
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "authorization.addAuthorization");
		model.addAttribute(KEY_FORM_ACTION, "saveAdd");

		return "/authorization/authorization_form";
	}

	@RequestMapping(value = "/saveAdd", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAdd(HttpServletRequest request, HttpServletResponse response,
			Authorization authorization)
	{
		checkInput(authorization);

		User user = WebUtils.getUser(request, response);

		authorization.setId(IDUtil.uuid());
		authorization.setCreateUser(user);

		this.authorizationService.add(user, authorization);

		return buildOperationMessageSaveSuccessResponseEntity(request);
	}

	@RequestMapping("/edit")
	public String edit(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id)
	{
		User user = WebUtils.getUser(request, response);

		setAuthorizationQueryContext(request);

		Authorization authorization = this.authorizationService.getByIdForEdit(user, id);

		setAppoiontResourceAttributeIf(request, model);
		model.addAttribute("authorization", authorization);
		model.addAttribute("user", user);
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "authorization.editAuthorization");
		model.addAttribute(KEY_FORM_ACTION, "saveEdit");

		return "/authorization/authorization_form";
	}

	@RequestMapping(value = "/saveEdit", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEdit(HttpServletRequest request, HttpServletResponse response,
			Authorization authorization)
	{
		if (isEmpty(authorization.getId()))
			throw new IllegalInputException();
		checkInput(authorization);

		User user = WebUtils.getUser(request, response);

		this.authorizationService.update(user, authorization);

		return buildOperationMessageSaveSuccessResponseEntity(request);
	}

	@RequestMapping("/view")
	public String view(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id)
	{
		User user = WebUtils.getUser(request, response);

		setAuthorizationQueryContext(request);

		Authorization authorization = this.authorizationService.getById(user, id);

		if (authorization == null)
			throw new RecordNotFoundException();

		setAppoiontResourceAttributeIf(request, model);
		model.addAttribute("authorization", authorization);
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "authorization.viewAuthorization");
		model.addAttribute(KEY_READONLY, true);

		return "/authorization/authorization_form";
	}

	@RequestMapping(value = "/delete", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> delete(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("id") String[] ids)
	{
		this.authorizationService.deleteByIds(WebUtils.getUser(request, response), ids);

		return buildOperationMessageDeleteSuccessResponseEntity(request);
	}

	@RequestMapping(value = "/query")
	public String query(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
	{
		setAppoiontResourceAttributeIf(request, model);
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "authorization.manageAuthorization");

		return "/authorization/authorization_grid";
	}

	@RequestMapping(value = "/queryData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public List<Authorization> queryData(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		String appointResource = getAppoiontResource(request);

		PagingQuery pagingQuery = getPagingQuery(request, null);

		setAuthorizationQueryContext(request);

		List<Authorization> authorizations = null;

		if (!isEmpty(appointResource))
			authorizations = this.authorizationService.queryForAppointResource(user, appointResource, pagingQuery);
		else
			authorizations = this.authorizationService.query(user, pagingQuery);

		return authorizations;
	}

	protected void setAppoiontResourceAttributeIf(HttpServletRequest request, org.springframework.ui.Model model)
	{
		String ap = getAppoiontResource(request);

		if (ap != null)
			model.addAttribute("appointResource", ap);
	}

	protected String getAppoiontResource(HttpServletRequest request)
	{
		return request.getParameter(PARAM_APPOINT_RESOURCE);
	}

	protected void setAuthorizationQueryContext(HttpServletRequest request)
	{
		AuthorizationQueryContext context = new AuthorizationQueryContext();
		context.setPrincipalAllLabel(getMessage(request, "authorization.principalType.ALL"));
		context.setPrincipalAnonymousLabel(getMessage(request, "authorization.principalType.ANONYMOUS"));
		context.setResourceType(Schema.AUTHORIZATION_RESOURCE_TYPE);

		AuthorizationQueryContext.set(context);
	}

	protected void checkInput(Authorization authorization) throws IllegalInputException
	{
		if (isEmpty(authorization.getResource()) || isEmpty(authorization.getResourceType())
				|| isEmpty(authorization.getPrincipal()) || isEmpty(authorization.getPrincipalType())
				|| authorization.getPermission() < Authorization.PERMISSION_NONE_START
				|| authorization.getPermission() > Authorization.PERMISSION_MAX)
			throw new IllegalInputException();
	}
}
