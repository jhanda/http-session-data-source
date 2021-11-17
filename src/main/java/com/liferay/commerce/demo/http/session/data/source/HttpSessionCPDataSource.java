/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.commerce.demo.http.session.data.source;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.commerce.product.catalog.CPQuery;
import com.liferay.commerce.product.data.source.CPDataSource;
import com.liferay.commerce.product.data.source.CPDataSourceResult;
import com.liferay.commerce.product.util.CPDefinitionHelper;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jeffrey Handa
 * @author Neil Griffin
 */
@Component(
	immediate = true,
	property = "commerce.product.data.source.name=" + HttpSessionCPDataSource.NAME,
	service = CPDataSource.class
)
public class HttpSessionCPDataSource implements CPDataSource {

	public static final String NAME = "http-session-data-source";

	@Override
	public String getLabel(Locale locale) {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", locale, getClass());

		return LanguageUtil.get(resourceBundle, "http-session-data-source");
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public CPDataSourceResult getResult(
			HttpServletRequest httpServletRequest, int start, int end)
		throws Exception {

		long companyId = _portal.getDefaultCompanyId();
		long globalGroupId = _groupLocalService.getCompanyGroup(
			companyId
		).getGroupId();
		long groupId = _portal.getScopeGroupId(httpServletRequest);

		HttpServletRequest origHttpServletRequest =
			_portal.getOriginalServletRequest(httpServletRequest);

		HttpSession httpSession = origHttpServletRequest.getSession();

		String age = (String)httpSession.getAttribute("age");

		if (Validator.isNull(age)) {
			age = "29";
			_log.warn("Http Session Attribute 'age' was null");
		}

		_log.debug("age=" + age);

		String householdIncome = (String)httpSession.getAttribute(
			"householdIncome");

		if (Validator.isNull(householdIncome)) {
			householdIncome = "49000.00";
			_log.warn("Http Session Attribute 'housholdIncome' was null");
		}

		_log.debug("householdIncome=" + householdIncome);

		CPQuery cpQuery = _getCPQuery(
			globalGroupId, Integer.valueOf(age),
			Double.valueOf(householdIncome));

		//SearchContext searchContext = new SearchContext();

		//return new CPDataSourceResult(new ArrayList<>(), 0);

		String finalCustomerSegment = householdIncome;

		return _cpDefinitionHelper.search(
			_portal.getScopeGroupId(httpServletRequest),
			new SearchContext() {
				{
					setAttributes(
						HashMapBuilder.<String, Serializable>put(
							Field.STATUS, WorkflowConstants.STATUS_APPROVED
						).build());
					setCompanyId(_portal.getCompanyId(httpServletRequest));
					// setKeywords(StringPool.STAR + finalCustomerSegment);
				}
			},
			cpQuery, start, end);
	}

	private CPQuery _getCPQuery(long groupId, int age, double householdIncome) {
		CPQuery cpQuery = new CPQuery();

		List<Long> categoryIds = new ArrayList<>();

		List<AssetCategory> assetCategories =
			_assetCategoryLocalService.getAssetCategories(
				QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		for (AssetCategory assetCategory : assetCategories) {
			String name = assetCategory.getName();

			if ((name.equals("29 and Under") && (age <= 29)) ||
				(name.equals("30 to 39") && (age >= 30) && (age <= 39)) ||
				(name.equals("40 to 49") && (age >= 40) && (age <= 49)) ||
				(name.equals("50 to 54") && (age >= 50) && (age <= 54)) ||
				(name.equals("65+") && (age >= 65))) {

				categoryIds.add(assetCategory.getCategoryId());

				_log.debug(
					"Adding category name=[" + name + "] categoryId=[" +
						assetCategory.getCategoryId() + "]");
			}
		}

		cpQuery.setAnyCategoryIds(
			categoryIds.stream(
			).mapToLong(
				l -> l
			).toArray());

		return cpQuery;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		HttpSessionCPDataSource.class);

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Reference
	private AssetEntryLocalService _assetEntryLocalService;

	@Reference
	private AssetTagLocalService _assetTagLocalService;

	@Reference
	private CPDefinitionHelper _cpDefinitionHelper;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private Portal _portal;

}