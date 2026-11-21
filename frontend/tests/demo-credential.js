import fs from 'node:fs/promises';

// Preserve the exact password, including legitimate leading/trailing spaces.
// The preferred driver input is an ephemeral process environment value.
export async function readDemoPassword(fallbackFile) {
  if (process.env.MODULE_A_DEMO_PASSWORD) return process.env.MODULE_A_DEMO_PASSWORD;
  const file = process.env.MODULE_A_DEMO_PASSWORD_FILE ?? fallbackFile;
  if (file) return fs.readFile(file, 'utf8');
  throw new Error('Configure the private demo credential in MODULE_A_DEMO_PASSWORD for browser tests.');
}
