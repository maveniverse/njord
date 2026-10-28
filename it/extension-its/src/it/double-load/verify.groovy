/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File log1f = new File( basedir, 'log-1.log' )
assert log1f.exists()
var log1 = log1f.text

assert log1.contains("[ERROR] Njord is loaded more than once")
assert log1.contains("extension3-${projectVersion}.jar")
assert log1.contains("extension3-0.9.10.jar")
// refused to run: nothing deployed/staged
assert !log1.contains("Created Njord artifact store")
assert !new File( basedir, "releases-repo").exists()
