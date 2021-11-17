package com.liferay.commerce.demo.http.session.data.source;

import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.commerce.product.catalog.CPQuery;
import com.liferay.commerce.product.data.source.CPDataSource;
import com.liferay.commerce.product.data.source.CPDataSourceResult;
import com.liferay.commerce.product.util.CPDefinitionHelper;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * @author Jeffrey Handa
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
    public CPDataSourceResult getResult(HttpServletRequest httpServletRequest, int start, int end) throws Exception {

        long companyId = _portal.getDefaultCompanyId();
        long globalGroupId = _groupLocalService.getCompanyGroup(companyId).getGroupId();
        long groupId = _portal.getScopeGroupId(httpServletRequest);

        String customerSegment = (String)httpServletRequest.getSession().getAttribute("LIFERAY_SHARED_customer_segment");

        if (Validator.isNull(customerSegment)){
            _log.debug("Http Session Attribute customer_segment is null");
            customerSegment = "basic";
        }

        if (customerSegment.equalsIgnoreCase("")){
            _log.debug("Http Session Attribute customer_segment is empty string");
            customerSegment = "basic";
        }

        CPQuery cpQuery = getCPQuery(globalGroupId, customerSegment);

        SearchContext searchContext = new SearchContext();

        //return new CPDataSourceResult(new ArrayList<>(), 0);

        String finalCustomerSegment = customerSegment;
        return _cpDefinitionHelper.search(
                _portal.getScopeGroupId(httpServletRequest),
                new SearchContext() {
                    {
                        setAttributes(
                                HashMapBuilder.<String, Serializable>put(
                                        Field.STATUS, WorkflowConstants.STATUS_APPROVED
                                ).build());
                        setCompanyId(_portal.getCompanyId(httpServletRequest));
                        setKeywords(
                                StringPool.STAR + finalCustomerSegment);                    }
                },
                cpQuery, start, end);


    }

    CPQuery getCPQuery(long groupId, String tag) throws PortalException {
        CPQuery cpQuery = new CPQuery();

        AssetTag assetTag = _assetTagLocalService.getTag(groupId, tag);
        long[] assetTags = new long[1];
        assetTags[0] = assetTag.getTagId();
        cpQuery.setAnyTagIds(assetTags);
        return cpQuery;


    }

    private static final Log _log = LogFactoryUtil.getLog(
            HttpSessionCPDataSource.class);

    @Reference
    private AssetEntryLocalService _assetEntryLocalService;

    @Reference
    private AssetTagLocalService _assetTagLocalService;

    @Reference
    private GroupLocalService _groupLocalService;
    @Reference
    private  CPDefinitionHelper _cpDefinitionHelper;


    @Reference
    private Portal _portal;


}
