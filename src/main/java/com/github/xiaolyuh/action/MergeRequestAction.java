package com.github.xiaolyuh.action;

import com.github.xiaolyuh.GitFlowPlus;
import com.github.xiaolyuh.i18n.I18n;
import com.github.xiaolyuh.i18n.I18nKey;
import com.github.xiaolyuh.ui.MergeRequestDialog;
import com.github.xiaolyuh.utils.CollectionUtils;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ReflectionUtil;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * merge request
 *
 * @author yuhao.wang3
 */
public class MergeRequestAction extends AnAction {
    protected GitFlowPlus gitFlowPlus = GitFlowPlus.getInstance();

    public MergeRequestAction() {
        super("Merge Request", "发起 code review", IconLoader.getIcon("/icons/mergeToTest.svg", Objects.requireNonNull(ReflectionUtil.getGrandCallerClass())));
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setText(I18n.getContent(I18nKey.MERGE_REQUEST_ACTION$TEXT));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();

        MergeRequestDialog mergeRequestDialog = new MergeRequestDialog(project);
        mergeRequestDialog.show();
        if (!mergeRequestDialog.isOK()) {
            return;
        }

        final String currentBranch = gitFlowPlus.getCurrentBranch(project);
        final String targetBranch = ConfigUtil.getConfig(project).get().getTestBranch();

        final GitRepository repository = GitBranchUtil.getCurrentRepository(project);
        if (Objects.isNull(repository)) {
            return;
        }

        new Task.Backgroundable(project, "Merge Request", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NotifyUtil.notifyGitCommand(event.getProject(), "===================================================================================");

                GitCommandResult result = gitFlowPlus.mergeRequest(repository, currentBranch, targetBranch, mergeRequestDialog.getMergeRequestOptions());
                if (!result.success()) {
                    NotifyUtil.notifyError(project, "Error", result.getErrorOutputAsJoinedString());
                }
                NotifyUtil.notifySuccess(project, "Success", result.getErrorOutputAsHtmlString());
                if (CollectionUtils.isNotEmpty(result.getErrorOutput()) && result.getErrorOutput().size() > 3) {
                    String address = result.getErrorOutput().get(2);
                    address = address.split("   ")[1];
                    NotifyUtil.notifySuccess(project, "Success", String.format("<a href=\"%s\">%s</a>", address, address));
                }

                // 刷新
                repository.update();
                myProject.getMessageBus().syncPublisher(GitRepository.GIT_REPO_CHANGE).repositoryChanged(repository);
                VirtualFileManager.getInstance().asyncRefresh(null);
            }
        }.queue();
    }


}


