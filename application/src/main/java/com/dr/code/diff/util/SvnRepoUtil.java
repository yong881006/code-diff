package com.dr.code.diff.util;

import com.dr.code.diff.vercontrol.svn.MySVNEditor;
import com.dr.common.errorcode.BizCode;
import com.dr.common.exception.BizException;
import com.dr.common.log.LoggerUtil;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.io.IOException;

/**
 * @ProjectName: code-diff-parent
 * @Package: com.dr.code.diff.util
 * @Description: java类作用描述
 * @Author: duanrui
 * @CreateDate: 2021/4/5 11:16
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
public class SvnRepoUtil {

    /**
     * @date:2021/4/5
     * @className:SvnRepoUtil
     * @author:Administrator
     * @description: 获取svn代码仓
     */
    public static void cloneRepository(String repoUrl, String codePath, String commitId, String userName, String password) {
        try {
            ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
            SVNUpdateClient updateClient = SVNClientManager.newInstance((DefaultSVNOptions) options, userName, password).getUpdateClient();
            updateClient.doCheckout(SVNURL.parseURIEncoded(repoUrl), new File(codePath), SVNRevision.create(Long.parseLong(commitId)), SVNRevision.create(Long.parseLong(commitId)), SVNDepth.INFINITY, false);
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }


    public static SVNDiffClient getSVNDiffClient(String userName, String password) {
        ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
        //实例化客户端管理类
        return SVNClientManager.newInstance((DefaultSVNOptions) options, userName, password).getDiffClient();
    }


    public static String getLocalDir(String gitUrl, String localBaseRepoDir, String version) {
        StringBuilder localDir = new StringBuilder(localBaseRepoDir);
        if (Strings.isNullOrEmpty(gitUrl)) {
            return "";
        }
        String repoName = Splitter.on("/")
                .splitToStream(gitUrl).reduce((first, second) -> second).get();
        localDir.append("/");
        localDir.append(repoName);
        localDir.append("/");
        localDir.append(version);
        return localDir.toString();
    }


}