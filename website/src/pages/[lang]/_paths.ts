import { LANGS } from '../../site';

export const langPaths = () => LANGS.map((lang) => ({ params: { lang } }));
