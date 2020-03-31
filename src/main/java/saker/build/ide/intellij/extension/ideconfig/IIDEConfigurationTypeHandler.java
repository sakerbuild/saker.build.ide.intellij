/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.build.ide.intellij.extension.ideconfig;

import com.intellij.openapi.progress.ProgressIndicator;
import saker.build.ide.intellij.api.ISakerProject;

import java.util.Map;

public interface IIDEConfigurationTypeHandler {
    public IIDEProjectConfigurationRootEntry[] parseConfiguration(ISakerProject project, Map<String, ?> configuration,
            ProgressIndicator monitor);
}
