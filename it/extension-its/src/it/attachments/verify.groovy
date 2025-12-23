/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
File imported = new File( basedir, 'imported-attachment.json' )
assert imported.exists()

File exported = new File( basedir, 'exported-attachment.json' )
assert exported.exists()

// Lets make strict assertion
// Also, consider Maven 3 vs 4 diff: they resolve differently; do not assert counts

assert imported.text == exported.text
