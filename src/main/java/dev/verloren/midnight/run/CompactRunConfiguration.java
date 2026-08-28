package dev.verloren.midnight.run;


import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CompactRunConfiguration extends LocatableConfigurationBase<CompactRunProfileState> {
  private String compactFilePath = "";
  private String outputDirectory = "";
  private boolean skipZk = true;
  private String customCompilerFlags = "";

  public CompactRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
    super(project, factory, name);
  }

  public String getCompactFilePath() {
    return compactFilePath;
  }

  public void setCompactFilePath(String compactFilePath) {
    String oldDerived = (this.compactFilePath != null && !this.compactFilePath.trim().isEmpty())
        ? CompactToolchainUtil.deriveOutputDirectory(getProject(), this.compactFilePath)
        : "";
    this.compactFilePath = compactFilePath != null ? compactFilePath : "";
    if (outputDirectory == null || outputDirectory.trim().isEmpty() || "gen".equals(outputDirectory.trim()) || outputDirectory.equals(oldDerived)) {
      if (!this.compactFilePath.trim().isEmpty()) {
        this.outputDirectory = CompactToolchainUtil.deriveOutputDirectory(getProject(), this.compactFilePath);
      }
    }
  }

  public String getOutputDirectory() {
    if (outputDirectory != null && !outputDirectory.trim().isEmpty()) {
      return outputDirectory;
    }
    if (compactFilePath != null && !compactFilePath.trim().isEmpty()) {
      return CompactToolchainUtil.deriveOutputDirectory(getProject(), compactFilePath);
    }
    return "gen";
  }

  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory != null ? outputDirectory : "";
  }

  public boolean isSkipZk() {
    return skipZk;
  }

  public void setSkipZk(boolean skipZk) {
    this.skipZk = skipZk;
  }

  public String getCustomCompilerFlags() {
    return customCompilerFlags;
  }

  public void setCustomCompilerFlags(String customCompilerFlags) {
    this.customCompilerFlags = customCompilerFlags != null ? customCompilerFlags : "";
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new CompactRunConfigurationEditor();
  }

  @Override
  public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
    return new CompactRunProfileState(environment, this);
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    compactFilePath = element.getAttributeValue("compactFilePath", "");
    String savedOutDir = element.getAttributeValue("outputDirectory", "");
    if (savedOutDir == null || savedOutDir.trim().isEmpty() || "gen".equals(savedOutDir.trim())) {
      if (compactFilePath != null && !compactFilePath.trim().isEmpty()) {
        outputDirectory = CompactToolchainUtil.deriveOutputDirectory(getProject(), compactFilePath);
      } else {
        outputDirectory = "gen";
      }
    } else {
      outputDirectory = savedOutDir;
    }
    skipZk = Boolean.parseBoolean(element.getAttributeValue("skipZk", "true"));
    customCompilerFlags = element.getAttributeValue("customCompilerFlags", "");
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    element.setAttribute("compactFilePath", compactFilePath != null ? compactFilePath : "");
    element.setAttribute("outputDirectory", getOutputDirectory());
    element.setAttribute("skipZk", String.valueOf(skipZk));
    element.setAttribute("customCompilerFlags", customCompilerFlags != null ? customCompilerFlags : "");
  }

  public @NotNull List<String> buildCommandLineArgs() {
    List<String> args = new ArrayList<>();
    args.add("--vscode");
    if (skipZk) {
      args.add("--skip-zk");
    }
    if (customCompilerFlags != null && !customCompilerFlags.trim().isEmpty()) {
      for (String flag : customCompilerFlags.trim().split("\\s+")) {
        if (!flag.isEmpty()) {
          args.add(flag);
        }
      }
    }
    if (compactFilePath != null && !compactFilePath.isEmpty()) {
      args.add(compactFilePath);
    }
    String outDir = getOutputDirectory();
    if (outDir != null && !outDir.isEmpty()) {
      args.add(outDir);
    }
    return args;
  }
}
